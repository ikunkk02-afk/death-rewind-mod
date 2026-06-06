package com.ikunkk02.deathrewind.rewind;

import com.ikunkk02.deathrewind.ModSounds;
import com.ikunkk02.deathrewind.component.ModComponents;
import com.ikunkk02.deathrewind.component.PlayerRewindComponent;
import com.ikunkk02.deathrewind.config.DeathRewindConfig;
import com.ikunkk02.deathrewind.network.ModNetworking;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class RewindManager {
	private static final int CLIENT_EFFECT_TICKS = 40;
	private static final Map<UUID, PlayerSnapshotBuffer<PlayerSnapshot>> SNAPSHOTS = new HashMap<>();
	private static final Map<UUID, Integer> REWIND_GENERATIONS = new HashMap<>();
	private static boolean registered;

	private RewindManager() {}

	public static void register() {
		if (registered) return;
		registered = true;
		ServerPlayerEvents.JOIN.register(RewindManager::onPlayerJoin);
		ServerPlayerEvents.LEAVE.register(RewindManager::onPlayerLeave);
		ServerTickEvents.END_SERVER_TICK.register(RewindManager::tick);
	}

	public static int getRewindGeneration(UUID playerUuid) {
		return REWIND_GENERATIONS.getOrDefault(playerUuid, 0);
	}

	public static boolean tryStartDeathRewind(ServerPlayer player, DamageSource source) {
		PlayerRewindComponent component = ModComponents.PLAYER_REWIND.get(player);
		DeathRewindConfig config = DeathRewindConfig.get();
		UUID uuid = player.getUUID();

		if (player.gameMode() != GameType.SURVIVAL) return false;

		boolean isHardcore = player.level().getLevelData().isHardcore();
		int effectiveMax = isHardcore ? config.effectiveMaxRewinds() : 0;
		if (isHardcore && effectiveMax > 0 && component.rewindCount() >= effectiveMax) return false;

		if (component.isRewinding()) {
			player.setHealth(Math.max(player.getHealth(), 1.0F));
			player.setDeltaMovement(Vec3.ZERO);
			return true;
		}

		if (config.enableCooldown() && component.cooldownTicks() > 0) return false;

		long currentGameTime = player.level().getGameTime();
		long targetGameTime = currentGameTime - config.rewindTicks();
		int currentGen = getRewindGeneration(uuid);
		PlayerSnapshotBuffer<PlayerSnapshot> buf = bufferFor(player);

		Optional<PlayerSnapshot> snapshot = buf.snapshotAtOrBefore(targetGameTime, currentGen);
		if (snapshot.isEmpty()) snapshot = buf.oldestAtOrBefore(targetGameTime);
		if (snapshot.isEmpty()) return false;

		PlayerSnapshot targetSnapshot = snapshot.orElseThrow();
		Vec3 startPos = player.position();

		component.setRewinding(true);
		player.setHealth(1.0F);
		player.setDeltaMovement(Vec3.ZERO);
		player.resetFallDistance();

		if (!targetSnapshot.restore(player)) {
			component.setRewinding(false);
			return false;
		}

		component.incrementRewindCount();
		component.setLastRewindGameTime(currentGameTime);
		component.setLastSafePos(player.blockPosition());
		component.setCooldownTicks(config.enableCooldown() ? config.cooldownTicks() : 0);
		player.invulnerableTime = Math.max(player.invulnerableTime, 200);
		player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 400, 1));
		player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));

		cleanupDroppedItems(player, config);

		player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
				ModSounds.REWIND_BELL, SoundSource.PLAYERS, 1.0F, 1.0F);

		if (isHardcore && effectiveMax > 0) {
			int remaining = Math.max(0, effectiveMax - component.rewindCount());
			player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
					String.format("§c☠ §7剩余回溯: §e%d§7/§e%d", remaining, effectiveMax)), false);
		}

		if (config.enableClientEffect()) {
			ModNetworking.sendRewindStart(player, CLIENT_EFFECT_TICKS, startPos, targetSnapshot.position());
		}

		if (!BlockRewindManager.startRestore(player, targetSnapshot, currentGameTime)) {
			finishRewind(player);
		}

		return true;
	}

	private static void tick(MinecraftServer server) {
		DeathRewindConfig config = DeathRewindConfig.get();

		for (UUID playerId : BlockRewindManager.tick(server)) {
			ServerPlayer player = server.getPlayerList().getPlayer(playerId);
			if (player != null) finishRewind(player);
		}

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			PlayerRewindComponent component = ModComponents.PLAYER_REWIND.get(player);
			component.tickCooldown();
			if (server.getTickCount() % 300 == 0 && !component.isRewinding()) {
				captureSnapshot(player, config);
			}
		}
	}

	private static void finishRewind(ServerPlayer player) {
		UUID uuid = player.getUUID();
		int newGen = REWIND_GENERATIONS.getOrDefault(uuid, 0) + 1;
		REWIND_GENERATIONS.put(uuid, newGen);

		BlockRewindManager.purgePlayerRecords(uuid);

		DeathRewindConfig config = DeathRewindConfig.get();
		PlayerRewindComponent component = ModComponents.PLAYER_REWIND.get(player);
		component.setLastSafePos(player.blockPosition());
		bufferFor(player, config).add(PlayerSnapshot.capture(player, player.level().getGameTime(), newGen));

		component.setRewinding(false);

		if (config.enableClientEffect()) {
			ModNetworking.sendRewindEnd(player, uuid);
		}
	}

	private static void cleanupDroppedItems(ServerPlayer player, DeathRewindConfig config) {
		int rewindTicks = config.rewindTicks();
		int radius = config.chunkRadius() * 16;
		AABB area = new AABB(player.blockPosition()).inflate(radius);

		for (ItemEntity item : ((ServerLevel) player.level()).getEntitiesOfClass(ItemEntity.class, area)) {
			if (item.getAge() < rewindTicks) item.discard();
		}
	}

	private static void onPlayerJoin(ServerPlayer player) {
		UUID uuid = player.getUUID();
		REWIND_GENERATIONS.putIfAbsent(uuid, 0);

		boolean worldIsHardcore = player.level().getLevelData().isHardcore();
		if (worldIsHardcore) DeathRewindConfig.lockForHardcore();
		else DeathRewindConfig.unlock();

		long currentGameTime = player.level().getGameTime();
		PlayerSnapshotBuffer<PlayerSnapshot> buffer = SNAPSHOTS.get(uuid);
		if (buffer != null) {
			Optional<PlayerSnapshot> latest = buffer.latest();
			if (latest.isPresent() && latest.get().gameTime() > currentGameTime + DeathRewindConfig.get().rewindTicks()) {
				SNAPSHOTS.remove(uuid);
				REWIND_GENERATIONS.put(uuid, 0);
				ModComponents.PLAYER_REWIND.get(player).setRewindCount(0);
			}
		}

		captureSnapshot(player, DeathRewindConfig.get());

		DeathRewindConfig config = DeathRewindConfig.get();
		PlayerRewindComponent component = ModComponents.PLAYER_REWIND.get(player);
		boolean isHardcore = player.level().getLevelData().isHardcore();
		String limitInfo;
		if (isHardcore && config.effectiveMaxRewinds() > 0) {
			int remaining = Math.max(0, config.effectiveMaxRewinds() - component.rewindCount());
			limitInfo = String.format("§c极限模式 §7剩余: §e%d§7/%d", remaining, config.effectiveMaxRewinds());
		} else {
			limitInfo = "§a无限次";
		}
		player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
				String.format("§6[死亡回溯] §7%s§7  回溯: §b%d秒§7  无敌: §e10秒",
						limitInfo, config.rewindSeconds())), false);
	}

	private static void onPlayerLeave(ServerPlayer player) {
		UUID uuid = player.getUUID();
		BlockRewindManager.cancel(uuid);
		ModComponents.PLAYER_REWIND.get(player).setRewinding(false);
	}

	private static void captureSnapshot(ServerPlayer player, DeathRewindConfig config) {
		PlayerRewindComponent component = ModComponents.PLAYER_REWIND.get(player);
		if (component.isRewinding() || player.isDeadOrDying()) return;
		UUID uuid = player.getUUID();
		int gen = getRewindGeneration(uuid);
		component.setLastSafePos(player.blockPosition());
		bufferFor(player, config).add(PlayerSnapshot.capture(player, player.level().getGameTime(), gen));
	}

	private static PlayerSnapshotBuffer<PlayerSnapshot> bufferFor(ServerPlayer player) {
		return bufferFor(player, DeathRewindConfig.get());
	}

	private static PlayerSnapshotBuffer<PlayerSnapshot> bufferFor(ServerPlayer player, DeathRewindConfig config) {
		return SNAPSHOTS.computeIfAbsent(player.getUUID(),
				uuid -> new PlayerSnapshotBuffer<>(Math.max(3, config.rewindSeconds() + 3),
						PlayerSnapshot::gameTime, PlayerSnapshot::rewindGeneration));
	}
}
