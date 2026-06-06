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
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;

import java.util.*;

public final class RewindManager {
	private static final int CLIENT_EFFECT_TICKS = 40;
	private static final Set<UUID> REWINDING = Collections.synchronizedSet(new HashSet<>());
	private static final Map<UUID, Map<BlockPos, CompoundTag>> PENDING_BE_RESTORE = new HashMap<>();
	private static boolean registered;

	private RewindManager() {}

	public static void register() {
		if (registered) return;
		registered = true;
		ServerPlayerEvents.JOIN.register(RewindManager::onPlayerJoin);
		ServerPlayerEvents.LEAVE.register(RewindManager::onPlayerLeave);
		ServerTickEvents.END_SERVER_TICK.register(RewindManager::tick);
	}

	public static boolean isRewinding(UUID playerUuid) {
		return REWINDING.contains(playerUuid);
	}

	// --- Death interception ---

	public static boolean tryStartDeathRewind(ServerPlayer player, net.minecraft.world.damagesource.DamageSource source) {
		PlayerRewindComponent component = ModComponents.PLAYER_REWIND.get(player);
		DeathRewindConfig config = DeathRewindConfig.get();
		UUID uuid = player.getUUID();

		if (player.gameMode() != GameType.SURVIVAL) return false;
		if (isRewinding(uuid)) {
			player.setHealth(Math.max(player.getHealth(), 1.0F));
			player.setDeltaMovement(Vec3.ZERO);
			return true;
		}

		boolean isHardcore = player.level().getLevelData().isHardcore();
		int effectiveMax = isHardcore ? config.effectiveMaxRewinds() : 0;
		if (isHardcore && effectiveMax > 0 && component.rewindCount() >= effectiveMax) return false;
		if (config.enableCooldown() && component.cooldownTicks() > 0) return false;

		int timelineId = CheckpointManager.getTimeline(uuid);
		long currentGameTime = player.level().getGameTime();
		long targetGameTime = currentGameTime - config.rewindTicks();

		Optional<RewindCheckpoint> cp = CheckpointManager.findRewindTarget(uuid, timelineId, targetGameTime);
		if (cp.isEmpty()) return false;

		RewindCheckpoint target = cp.get();
		Vec3 startPos = player.position();

		// Lock player during rewind
		REWINDING.add(uuid);
		component.setRewinding(true);
		player.setHealth(1.0F);
		player.setDeltaMovement(Vec3.ZERO);
		player.resetFallDistance();

		// Restore player state from checkpoint
		if (!target.playerSnapshot().restore(player)) {
			REWINDING.remove(uuid);
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

		// Restore world time to checkpoint
		var clockRegistry = player.level().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.WORLD_CLOCK);
		var overworldClock = clockRegistry.get(net.minecraft.resources.Identifier.withDefaultNamespace("overworld")).orElse(null);
		if (overworldClock != null) {
			player.level().clockManager().setTotalTicks(overworldClock, target.dayTime());
		}

		// Schedule block entity NBT restore (applied after block rewind completes)
		Map<BlockPos, CompoundTag> beNbt = BlockEntitySnapshotManager.findSnapshot(uuid, target.checkpointGameTime()).orElse(null);
		if (beNbt != null) {
			PENDING_BE_RESTORE.put(uuid, beNbt);
		}

		player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
				ModSounds.REWIND_BELL, SoundSource.PLAYERS, 1.0F, 1.0F);

		if (isHardcore && effectiveMax > 0) {
			int remaining = Math.max(0, effectiveMax - component.rewindCount());
			player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
					String.format("§c☠ §7剩余回溯: §e%d§7/§e%d", remaining, effectiveMax)), false);
		}

		if (config.enableClientEffect()) {
			ModNetworking.sendRewindStart(player, CLIENT_EFFECT_TICKS, startPos, target.playerSnapshot().position());
		}

		// Start block rewind using checkpoint's blockChangeLogIndex
		if (!BlockRewindManager.startRestoreFromCheckpoint(player, target, currentGameTime)) {
			finishRewind(player);
		}

		return true;
	}

	// --- Tick ---

	private static void tick(MinecraftServer server) {
		DeathRewindConfig config = DeathRewindConfig.get();

		for (UUID playerId : BlockRewindManager.tick(server)) {
			ServerPlayer player = server.getPlayerList().getPlayer(playerId);
			if (player != null) finishRewind(player);
		}

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			PlayerRewindComponent component = ModComponents.PLAYER_REWIND.get(player);
			component.tickCooldown();

			if (server.getTickCount() % config.checkpointIntervalTicks() == 0 && !isRewinding(player.getUUID())) {
				int blockIndex = CheckpointManager.nextBlockLogIndex(player.getUUID());
				CheckpointManager.createCheckpoint(player, blockIndex);
				// Snapshot all block entities at the same time as checkpoint
				BlockEntitySnapshotManager.takeSnapshot(player);
				// Notify client HUD via action bar (auto-fades, doesn't spam chat)
				if (config.enableSaveNotification()) {
					player.sendSystemMessage(
							net.minecraft.network.chat.Component.literal("§a● §7存档点已保存"),
							true
					);
				}
			}
		}
	}

	// --- Finish rewind: advance timeline ---

	private static void finishRewind(ServerPlayer player) {
		UUID uuid = player.getUUID();

		// Advance to new timeline — clears old checkpoints, old block records
		CheckpointManager.advanceTimeline(uuid);

		// Purge old timeline's block records
		BlockRewindManager.purgePlayerRecords(uuid);

		PlayerRewindComponent component = ModComponents.PLAYER_REWIND.get(player);
		component.setLastSafePos(player.blockPosition());

		// Create initial checkpoint for new timeline
		int blockIndex = CheckpointManager.nextBlockLogIndex(uuid);
		CheckpointManager.createCheckpoint(player, blockIndex);

		// Apply pending block entity NBT restore
		Map<BlockPos, CompoundTag> beNbt = PENDING_BE_RESTORE.remove(uuid);
		if (beNbt != null) {
			BlockEntitySnapshotManager.restoreBlockEntities((ServerLevel) player.level(), beNbt);
		}

		// Unlock
		REWINDING.remove(uuid);
		component.setRewinding(false);

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
			if (item.getAge() < rewindTicks) item.discard();
		}
	}

	// --- Player join/leave ---

	private static void onPlayerJoin(ServerPlayer player) {
		UUID uuid = player.getUUID();
		CheckpointManager.advanceTimeline(uuid); // ensure fresh timeline

		boolean worldIsHardcore = player.level().getLevelData().isHardcore();
		if (worldIsHardcore) DeathRewindConfig.lockForHardcore();
		else DeathRewindConfig.unlock();

		// Create first checkpoint
		int blockIndex = CheckpointManager.nextBlockLogIndex(uuid);
		CheckpointManager.createCheckpoint(player, blockIndex);

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
		REWINDING.remove(uuid);
		BlockRewindManager.cancel(uuid);
		BlockEntitySnapshotManager.clearPlayer(uuid);
		ModComponents.PLAYER_REWIND.get(player).setRewinding(false);
	}
}
