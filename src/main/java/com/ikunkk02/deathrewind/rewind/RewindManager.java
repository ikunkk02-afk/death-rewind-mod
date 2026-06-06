package com.ikunkk02.deathrewind.rewind;

import com.ikunkk02.deathrewind.ModSounds;
import com.ikunkk02.deathrewind.component.ModComponents;
import com.ikunkk02.deathrewind.component.PlayerRewindComponent;
import com.ikunkk02.deathrewind.config.DeathRewindConfig;
import com.ikunkk02.deathrewind.network.ModNetworking;
import com.ikunkk02.deathrewind.rewind.checkpoint.CheckpointManager;
import com.ikunkk02.deathrewind.rewind.checkpoint.RewindCheckpoint;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class RewindManager {
	private static final int CLIENT_EFFECT_TICKS = 40;
	private static final Set<UUID> REWINDING = Collections.synchronizedSet(new HashSet<>());
	private static final Map<UUID, Map<BlockPos, CompoundTag>> PENDING_BE_RESTORE = new HashMap<>();
	private static boolean registered;

	private RewindManager() {
	}

	public static void register() {
		if (registered) {
			return;
		}

		registered = true;
		ServerPlayerEvents.JOIN.register(RewindManager::onPlayerJoin);
		ServerPlayerEvents.LEAVE.register(RewindManager::onPlayerLeave);
		ServerTickEvents.END_SERVER_TICK.register(RewindManager::tick);
	}

	public static boolean isRewinding(UUID playerUuid) {
		return REWINDING.contains(playerUuid);
	}

	// --- Death interception ---

	public static boolean tryStartDeathRewind(ServerPlayer player, DamageSource source) {
		PlayerRewindComponent component = ModComponents.PLAYER_REWIND.get(player);
		DeathRewindConfig config = DeathRewindConfig.get();
		UUID uuid = player.getUUID();

		if (player.gameMode() != GameType.SURVIVAL) {
			return false;
		}

		if (isRewinding(uuid)) {
			player.setHealth(Math.max(player.getHealth(), 1.0F));
			player.setDeltaMovement(Vec3.ZERO);
			return true;
		}

		boolean isHardcore = player.level().getLevelData().isHardcore();
		int effectiveMax = isHardcore ? config.effectiveMaxRewinds() : 0;
		if (isHardcore && effectiveMax > 0 && component.rewindCount() >= effectiveMax) {
			return false;
		}

		if (config.enableCooldown() && component.cooldownTicks() > 0) {
			return false;
		}

		int timelineId = CheckpointManager.getTimeline(uuid);
		long currentGameTime = player.level().getGameTime();
		long targetGameTime = currentGameTime - config.rewindTicks();

		Optional<RewindCheckpoint> cp = CheckpointManager.findRewindTarget(uuid, timelineId, targetGameTime);
		if (cp.isEmpty()) {
			return false;
		}

		RewindCheckpoint target = cp.get();
		Vec3 startPos = player.position();

		// Lock player during rewind.
		REWINDING.add(uuid);
		component.setRewinding(true);
		player.setHealth(1.0F);
		player.setDeltaMovement(Vec3.ZERO);
		player.resetFallDistance();

		// Restore player state from checkpoint.
		if (!target.playerSnapshot().restore(player)) {
			REWINDING.remove(uuid);
			component.setRewinding(false);
			return false;
		}

		component.incrementRewindCount();
		component.setLastRewindGameTime(currentGameTime);
		component.setLastSafePos(player.blockPosition());
		component.setCooldownTicks(config.enableCooldown() ? config.cooldownTicks() : 0);
		player.invulnerableTime = Math.max(player.invulnerableTime, config.invulnerableTicks());
		player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 400, 1));
		player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));

		cleanupDroppedItems(player, config);

		// Restore world time to checkpoint.
		var clockRegistry = player.level().registryAccess().lookupOrThrow(Registries.WORLD_CLOCK);
		var overworldClock = clockRegistry.get(Identifier.withDefaultNamespace("overworld")).orElse(null);
		if (overworldClock != null) {
			player.level().clockManager().setTotalTicks(overworldClock, target.dayTime());
		}

		// Schedule block entity NBT restore after block rewind completes.
		Map<BlockPos, CompoundTag> beNbt = BlockEntitySnapshotManager.findSnapshot(
				uuid,
				target.checkpointGameTime()
		).orElse(null);
		if (beNbt != null) {
			PENDING_BE_RESTORE.put(uuid, beNbt);
		}

		player.level().playSound(
				null,
				player.getX(),
				player.getY(),
				player.getZ(),
				ModSounds.REWIND_BELL,
				SoundSource.PLAYERS,
				1.0F,
				1.0F
		);

		if (isHardcore && effectiveMax > 0) {
			int remaining = Math.max(0, effectiveMax - component.rewindCount());
			player.sendSystemMessage(
					Component.literal(String.format(
							"[死亡回溯] 极限模式剩余回溯次数：%d/%d",
							remaining,
							effectiveMax
					)),
					false
			);
		}

		if (config.enableClientEffect()) {
			ModNetworking.sendRewindStart(player, CLIENT_EFFECT_TICKS, startPos, target.playerSnapshot().position());
		}

		// Start block rewind using checkpoint's blockChangeLogIndex.
		if (!BlockRewindManager.startRestoreFromCheckpoint(player, target)) {
			finishRewind(player);
		}

		return true;
	}

	// --- Tick ---

	private static void tick(MinecraftServer server) {
		DeathRewindConfig config = DeathRewindConfig.get();

		for (UUID playerId : BlockRewindManager.tick(server)) {
			ServerPlayer player = server.getPlayerList().getPlayer(playerId);
			if (player != null) {
				finishRewind(player);
			}
		}

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			PlayerRewindComponent component = ModComponents.PLAYER_REWIND.get(player);
			component.tickCooldown();

			if (server.getTickCount() % config.checkpointIntervalTicks() == 0 && !isRewinding(player.getUUID())) {
				int blockIndex = CheckpointManager.nextBlockLogIndex(player.getUUID());
				CheckpointManager.createCheckpoint(player, blockIndex);

				// Snapshot block entities at the same time as the checkpoint.
				BlockEntitySnapshotManager.takeSnapshot(player);

				if (config.enableSaveNotification()) {
					player.sendSystemMessage(
							Component.literal("§8[§a●§8] §7存档点已保存"),
							true
					);
				}
			}
		}
	}

	// --- Finish rewind: advance timeline ---

	private static void finishRewind(ServerPlayer player) {
		UUID uuid = player.getUUID();

		// Advance to a new timeline and clear stale checkpoint/block records.
		CheckpointManager.advanceTimeline(uuid);
		BlockRewindManager.purgePlayerRecords(uuid);

		PlayerRewindComponent component = ModComponents.PLAYER_REWIND.get(player);
		component.setLastSafePos(player.blockPosition());

		Map<BlockPos, CompoundTag> beNbt = PENDING_BE_RESTORE.remove(uuid);
		if (beNbt != null) {
			BlockEntitySnapshotManager.restoreBlockEntities((ServerLevel) player.level(), beNbt);
		}

		// Unlock before taking the first snapshot of the new timeline.
		REWINDING.remove(uuid);
		component.setRewinding(false);

		// Create the initial checkpoint for the new timeline after block entities are restored.
		int blockIndex = CheckpointManager.nextBlockLogIndex(uuid);
		CheckpointManager.createCheckpoint(player, blockIndex);
		BlockEntitySnapshotManager.takeSnapshot(player);

		if (DeathRewindConfig.get().enableClientEffect()) {
			ModNetworking.sendRewindEnd(player, uuid);
		}
	}

	// --- Dropped item cleanup ---

	private static void cleanupDroppedItems(ServerPlayer player, DeathRewindConfig config) {
		int rewindTicks = config.rewindTicks();
		int radius = config.chunkRadius() * 16;
		AABB area = new AABB(player.blockPosition()).inflate(radius);
		for (ItemEntity item : ((ServerLevel) player.level()).getEntitiesOfClass(ItemEntity.class, area)) {
			if (item.getAge() < rewindTicks) {
				item.discard();
			}
		}
	}

	// --- Player join/leave ---

	private static void onPlayerJoin(ServerPlayer player) {
		UUID uuid = player.getUUID();
		CheckpointManager.advanceTimeline(uuid);

		boolean worldIsHardcore = player.level().getLevelData().isHardcore();
		if (worldIsHardcore) {
			DeathRewindConfig.lockForHardcore();
		} else {
			DeathRewindConfig.unlock();
		}

		int blockIndex = CheckpointManager.nextBlockLogIndex(uuid);
		CheckpointManager.createCheckpoint(player, blockIndex);

		DeathRewindConfig config = DeathRewindConfig.get();
		PlayerRewindComponent component = ModComponents.PLAYER_REWIND.get(player);
		boolean isHardcore = player.level().getLevelData().isHardcore();
		String limitInfo;
		if (isHardcore && config.effectiveMaxRewinds() > 0) {
			int remaining = Math.max(0, config.effectiveMaxRewinds() - component.rewindCount());
			limitInfo = String.format(
					"极限剩余：%d/%d",
					remaining,
					config.effectiveMaxRewinds()
			);
		} else {
			limitInfo = "无限回溯";
		}

		player.sendSystemMessage(
				Component.literal(String.format(
						"[死亡回溯] %s | 回溯：%d秒 | 无敌：%.1f秒",
						limitInfo,
						config.rewindSeconds(),
						config.invulnerableTicks() / 20.0F
				)),
				false
		);
	}

	private static void onPlayerLeave(ServerPlayer player) {
		UUID uuid = player.getUUID();
		REWINDING.remove(uuid);
		BlockRewindManager.cancel(uuid);
		BlockEntitySnapshotManager.clearPlayer(uuid);
		ModComponents.PLAYER_REWIND.get(player).setRewinding(false);
	}
}
