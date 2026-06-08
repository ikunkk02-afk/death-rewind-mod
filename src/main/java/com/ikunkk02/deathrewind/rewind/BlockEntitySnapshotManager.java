package com.ikunkk02.deathrewind.rewind;

import com.ikunkk02.deathrewind.config.DeathRewindConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Periodically snapshots block entity NBT within player chunk radius.
 * Used to restore container inventories (chests, furnaces, etc.) on rewind.
 */
public final class BlockEntitySnapshotManager {
	/** playerUuid -> snapshot list (ordered by gameTime) */
	private static final Map<UUID, List<BlockEntitySnapshot>> SNAPSHOTS = new HashMap<>();

	private BlockEntitySnapshotManager() {
	}

	public static void takeSnapshot(ServerPlayer player) {
		DeathRewindConfig config = DeathRewindConfig.get();
		if (player.isDeadOrDying()) {
			return;
		}

		if (RewindManager.isRewinding(player.getUUID())) {
			return;
		}

		UUID uuid = player.getUUID();
		long gameTime = player.level().getGameTime();
		ChunkPos center = player.chunkPosition();
		int radius = config.chunkRadius();

		Map<BlockPos, CompoundTag> blockEntities = new HashMap<>();
		ServerLevel level = (ServerLevel) player.level();

		for (int cx = -radius; cx < radius; cx++) {
			for (int cz = -radius; cz < radius; cz++) {
				ChunkPos chunkPos = new ChunkPos(center.x() + cx, center.z() + cz);
				if (!level.hasChunk(chunkPos.x(), chunkPos.z())) {
					continue;
				}

				for (BlockEntity be : level.getChunk(chunkPos.x(), chunkPos.z()).getBlockEntities().values()) {
					CompoundTag tag = be.saveWithFullMetadata(level.registryAccess());
					blockEntities.put(be.getBlockPos().immutable(), tag);
				}
			}
		}

		if (!blockEntities.isEmpty()) {
			SNAPSHOTS.computeIfAbsent(uuid, k -> new ArrayList<>())
					.add(new BlockEntitySnapshot(gameTime, blockEntities));

			// Prune old snapshots
			long cutoff = gameTime - config.rewindTicks() * 2;
			List<BlockEntitySnapshot> list = SNAPSHOTS.get(uuid);
			if (list != null) {
				list.removeIf(s -> s.gameTime < cutoff);
			}
		}
	}

	/**
	 * Find the most recent block entity snapshot at or before targetGameTime.
	 * Returns the block entity NBT map to restore.
	 */
	public static Optional<Map<BlockPos, CompoundTag>> findSnapshot(UUID playerUuid, long targetGameTime) {
		List<BlockEntitySnapshot> list = SNAPSHOTS.get(playerUuid);
		if (list == null || list.isEmpty()) {
			return Optional.empty();
		}

		BlockEntitySnapshot candidate = null;
		for (BlockEntitySnapshot s : list) {
			if (s.gameTime <= targetGameTime) {
				candidate = s;
			} else {
				break;
			}
		}
		if (candidate != null) {
			return Optional.of(candidate.blockEntities);
		}

		// Fallback: oldest snapshot
		return Optional.of(list.get(0).blockEntities);
	}

	/** Restore block entities from the snapshot. */
	public static void restoreBlockEntities(ServerLevel level, Map<BlockPos, CompoundTag> savedEntities) {
		for (Map.Entry<BlockPos, CompoundTag> entry : savedEntities.entrySet()) {
			BlockPos pos = entry.getKey();
			if (!level.isLoaded(pos)) {
				continue;
			}

			CompoundTag tag = entry.getValue().copy();
			// Re-create block entity from saved NBT
			BlockEntity newBe = BlockEntity.loadStatic(pos, level.getBlockState(pos), tag, level.registryAccess());
			if (newBe != null) {
				level.removeBlockEntity(pos);
				level.setBlockEntity(newBe);
				newBe.setChanged();
				level.blockEntityChanged(pos);
			}
		}
	}

	public static void clearPlayer(UUID playerUuid) {
		SNAPSHOTS.remove(playerUuid);
	}

	private record BlockEntitySnapshot(long gameTime, Map<BlockPos, CompoundTag> blockEntities) {
	}
}
