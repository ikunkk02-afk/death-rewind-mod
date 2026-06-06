package com.ikunkk02.deathrewind.rewind.checkpoint;

import com.ikunkk02.deathrewind.config.DeathRewindConfig;
import com.ikunkk02.deathrewind.rewind.PlayerSnapshot;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Manages checkpoints per player, per timeline.
 * Creates checkpoints at regular intervals and prunes old ones.
 * Each player has their own independent timeline.
 */
public final class CheckpointManager {
	/** playerUuid -> timelineId -> checkpoints (ordered by gameTime) */
	private static final Map<UUID, Map<Integer, List<RewindCheckpoint>>> CHECKPOINTS = new HashMap<>();
	/** playerUuid -> current timelineId */
	private static final Map<UUID, Integer> TIMELINES = new HashMap<>();
	/** playerUuid -> blockChangeLogIndex counter */
	private static final Map<UUID, Integer> BLOCK_LOG_INDICES = new HashMap<>();

	private CheckpointManager() {
	}

	// --- Timeline ---

	public static int getTimeline(UUID playerUuid) {
		return TIMELINES.getOrDefault(playerUuid, 0);
	}

	public static int advanceTimeline(UUID playerUuid) {
		int next = TIMELINES.getOrDefault(playerUuid, 0) + 1;
		TIMELINES.put(playerUuid, next);

		Map<Integer, List<RewindCheckpoint>> playerCheckpoints = CHECKPOINTS.get(playerUuid);
		if (playerCheckpoints != null) {
			playerCheckpoints.keySet().removeIf(timelineId -> timelineId < next);
		}

		return next;
	}

	// --- Block change log index ---

	public static int nextBlockLogIndex(UUID playerUuid) {
		int index = BLOCK_LOG_INDICES.getOrDefault(playerUuid, 0);
		BLOCK_LOG_INDICES.put(playerUuid, index + 1);
		return index;
	}

	public static int currentBlockLogIndex(UUID playerUuid) {
		return BLOCK_LOG_INDICES.getOrDefault(playerUuid, 0);
	}

	// --- Checkpoints ---

	public static void createCheckpoint(ServerPlayer player, int blockChangeLogIndex) {
		DeathRewindConfig config = DeathRewindConfig.get();
		UUID uuid = player.getUUID();
		int timelineId = getTimeline(uuid);
		long gameTime = player.level().getGameTime();
		long dayTime = player.level().getOverworldClockTime();

		PlayerSnapshot snapshot = PlayerSnapshot.capture(player, gameTime, timelineId);
		RewindCheckpoint checkpoint = new RewindCheckpoint(
				uuid,
				timelineId,
				gameTime,
				dayTime,
				snapshot,
				blockChangeLogIndex,
				player.level().dimension(),
				player.chunkPosition(),
				config.chunkRadius()
		);

		CHECKPOINTS.computeIfAbsent(uuid, key -> new HashMap<>())
				.computeIfAbsent(timelineId, key -> new ArrayList<>())
				.add(checkpoint);

		pruneOldCheckpoints(uuid, timelineId);
	}

	/**
	 * Find the checkpoint closest to (currentGameTime - rewindTicks) in the current timeline.
	 * If no checkpoint is old enough, returns the earliest checkpoint in the timeline.
	 */
	public static Optional<RewindCheckpoint> findRewindTarget(UUID playerUuid, int timelineId, long targetGameTime) {
		Map<Integer, List<RewindCheckpoint>> playerCheckpoints = CHECKPOINTS.get(playerUuid);
		if (playerCheckpoints == null) {
			return Optional.empty();
		}

		List<RewindCheckpoint> timelineCheckpoints = playerCheckpoints.get(timelineId);
		if (timelineCheckpoints == null || timelineCheckpoints.isEmpty()) {
			return Optional.empty();
		}

		RewindCheckpoint candidate = null;
		for (RewindCheckpoint checkpoint : timelineCheckpoints) {
			if (checkpoint.checkpointGameTime() <= targetGameTime) {
				candidate = checkpoint;
			} else {
				break;
			}
		}

		if (candidate != null) {
			return Optional.of(candidate);
		}

		return Optional.of(timelineCheckpoints.get(0));
	}

	/** Purge old checkpoints and keep only the last 4 per timeline. */
	private static void pruneOldCheckpoints(UUID uuid, int timelineId) {
		Map<Integer, List<RewindCheckpoint>> playerCheckpoints = CHECKPOINTS.get(uuid);
		if (playerCheckpoints == null) {
			return;
		}

		List<RewindCheckpoint> checkpoints = playerCheckpoints.get(timelineId);
		if (checkpoints == null) {
			return;
		}

		while (checkpoints.size() > 4) {
			checkpoints.remove(0);
		}
	}

	/** Clear all data for a player. */
	public static void clearPlayer(UUID playerUuid) {
		CHECKPOINTS.remove(playerUuid);
		TIMELINES.remove(playerUuid);
		BLOCK_LOG_INDICES.remove(playerUuid);
	}

	/** Clear old timeline data and advance to next timeline. */
	public static void clearOldTimeline(UUID playerUuid) {
		Map<Integer, List<RewindCheckpoint>> playerCheckpoints = CHECKPOINTS.get(playerUuid);
		if (playerCheckpoints != null) {
			int current = getTimeline(playerUuid);
			playerCheckpoints.keySet().removeIf(timelineId -> timelineId < current);
		}
	}
}
