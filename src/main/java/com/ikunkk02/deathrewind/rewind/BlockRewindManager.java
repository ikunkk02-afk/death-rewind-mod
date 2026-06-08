package com.ikunkk02.deathrewind.rewind;

import com.ikunkk02.deathrewind.config.DeathRewindConfig;
import com.ikunkk02.deathrewind.network.ModNetworking;
import com.ikunkk02.deathrewind.rewind.checkpoint.CheckpointManager;
import com.ikunkk02.deathrewind.rewind.checkpoint.RewindCheckpoint;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class BlockRewindManager {
	private static final Identifier RESTORE_PARTICLE = Identifier.withDefaultNamespace("reverse_portal");
	private static final Deque<BlockChangeRecord> RECORDS = new ArrayDeque<>();
	private static final Map<UUID, RewindExecutor> EXECUTORS = new HashMap<>();
	private static boolean restoring;

	private BlockRewindManager() {
	}

	// --- Record block changes ---

	public static void recordBeforeChange(ServerLevel level, BlockPos pos, BlockState newState) {
		DeathRewindConfig config = DeathRewindConfig.get();
		if (!config.enableBlockRewind() || restoring) {
			return;
		}

		// C2ME and some optimization mods may call block changes off the main server thread.
		// In compatibility mode, do not touch player lists or block entities from those
		// threads; missing one async record is much better than occasionally blocking
		// normal block placement.
		MinecraftServer server = level.getServer();
		if (config.optimizationModCompatibility() && server != null && !server.isSameThread()) {
			return;
		}

		if (level.isOutsideBuildHeight(pos) || !level.isLoaded(pos)) {
			return;
		}

		BlockState previousState = level.getBlockState(pos);
		if (previousState.equals(newState)) {
			return;
		}

		ResourceKey<Level> dimension = level.dimension();
		ChunkPos changedChunk = ChunkPos.containing(pos);
		int radius = config.chunkRadius();
		List<RecordTarget> targets = new ArrayList<>();

		for (ServerPlayer player : level.players()) {
			if (player.isDeadOrDying()) {
				continue;
			}

			if (!RewindManager.isRewinding(player.getUUID()) && player.gameMode() != GameType.SURVIVAL) {
				continue;
			}

			if (!isInChunkWindow(player.chunkPosition(), changedChunk, radius)) {
				continue;
			}

			UUID playerUuid = player.getUUID();
			targets.add(new RecordTarget(
					playerUuid,
					CheckpointManager.getTimeline(playerUuid),
					CheckpointManager.currentBlockLogIndex(playerUuid)
			));
		}

		if (targets.isEmpty()) {
			return;
		}

		BlockEntity blockEntity = level.getBlockEntity(pos);
		CompoundTag blockEntityNbt = blockEntity == null ? null : blockEntity.saveWithFullMetadata(level.registryAccess());
		long gameTime = level.getGameTime();

		for (RecordTarget target : targets) {
			RECORDS.addLast(new BlockChangeRecord(gameTime, dimension, pos, previousState, blockEntityNbt,
					target.playerUuid(), target.timelineId(), target.blockLogIndex()));
		}

		long cutoff = gameTime - config.rewindTicks() * 2; // keep extra buffer
		while (!RECORDS.isEmpty() && RECORDS.peekFirst().gameTime() < cutoff) {
			RECORDS.removeFirst();
		}
	}

	// --- Start restore from checkpoint ---

	public static boolean startRestoreFromCheckpoint(ServerPlayer player, RewindCheckpoint checkpoint) {
		DeathRewindConfig config = DeathRewindConfig.get();
		if (!config.enableBlockRewind()) {
			return false;
		}

		UUID playerUuid = player.getUUID();
		int logIndex = checkpoint.blockChangeLogIndex();
		int timelineId = checkpoint.timelineId();
		ChunkPos centerChunk = checkpoint.centerChunk();
		ResourceKey<Level> dimension = checkpoint.dimension();
		int radius = config.chunkRadius();

		List<BlockChangeRecord> matches = new ArrayList<>();
		Iterator<BlockChangeRecord> descending = RECORDS.descendingIterator();

		while (descending.hasNext()) {
			BlockChangeRecord record = descending.next();
			if (record.blockChangeLogIndex() < logIndex) break;

			if (record.timelineId() == timelineId
					&& record.playerUuid().equals(playerUuid)
					&& record.dimension().equals(dimension)
					&& isInChunkWindow(centerChunk, record.pos(), radius)) {
				matches.add(record);
			}
		}

		if (matches.isEmpty()) {
			return false;
		}

		EXECUTORS.put(playerUuid, new RewindExecutor(playerUuid, dimension, matches));
		return true;
	}

	// --- Tick restore ---

	public static List<UUID> tick(MinecraftServer server) {
		DeathRewindConfig config = DeathRewindConfig.get();
		List<UUID> completed = new ArrayList<>();
		Iterator<Map.Entry<UUID, RewindExecutor>> iterator = EXECUTORS.entrySet().iterator();

		while (iterator.hasNext()) {
			RewindExecutor executor = iterator.next().getValue();
			if (executor.tick(server, config)) {
				completed.add(executor.playerId());
				iterator.remove();
			}
		}

		return completed;
	}

	public static void cancel(UUID playerId) {
		EXECUTORS.remove(playerId);
	}

	public static void purgePlayerRecords(UUID playerUuid) {
		RECORDS.removeIf(record -> record.playerUuid().equals(playerUuid));
	}

	// --- Single block restore ---

	static void restoreRecord(ServerLevel level, BlockChangeRecord record) {
		if (!level.dimension().equals(record.dimension())
				|| level.isOutsideBuildHeight(record.pos())
				|| !level.isLoaded(record.pos())) {
			return;
		}

		restoring = true;
		try {
			level.setBlock(record.pos(), record.previousState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
			restoreBlockEntity(level, record);
		} finally {
			restoring = false;
		}

		// Particles
		double x = record.pos().getX() + 0.5D;
		double y = record.pos().getY() + 0.5D;
		double z = record.pos().getZ() + 0.5D;
		level.sendParticles(ParticleTypes.REVERSE_PORTAL, x, y, z, 8, 0.35D, 0.35D, 0.35D, 0.03D);
		ModNetworking.sendBlockParticle(level, record.pos(), RESTORE_PARTICLE);
	}

	private static void restoreBlockEntity(ServerLevel level, BlockChangeRecord record) {
		CompoundTag nbt = record.previousBlockEntityNbt();
		if (nbt == null) {
			level.removeBlockEntity(record.pos());
			return;
		}
		BlockEntity blockEntity = BlockEntity.loadStatic(record.pos(), record.previousState(), nbt.copy(), level.registryAccess());
		if (blockEntity == null) {
			level.removeBlockEntity(record.pos());
			return;
		}
		level.setBlockEntity(blockEntity);
		blockEntity.setChanged();
		level.blockEntityChanged(record.pos());
	}

	private static boolean isInChunkWindow(ChunkPos center, BlockPos pos, int radius) {
		return isInChunkWindow(center, ChunkPos.containing(pos), radius);
	}

	private static boolean isInChunkWindow(ChunkPos center, ChunkPos target, int radius) {
		int dx = target.x() - center.x();
		int dz = target.z() - center.z();
		return dx >= -radius && dx < radius && dz >= -radius && dz < radius;
	}

	private record RecordTarget(UUID playerUuid, int timelineId, int blockLogIndex) {
	}
}
