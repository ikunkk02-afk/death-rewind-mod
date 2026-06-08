package com.ikunkk02.deathrewind.rewind.checkpoint;

import com.ikunkk02.deathrewind.rewind.PlayerSnapshot;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.UUID;

/**
 * A checkpoint recording a player's state at a specific game time.
 * Used to restore the player and block state on death rewind.
 */
public record RewindCheckpoint(
		UUID playerUuid,
		int timelineId,
		long checkpointGameTime,
		long dayTime,
		PlayerSnapshot playerSnapshot,
		int blockChangeLogIndex,
		ResourceKey<Level> dimension,
		ChunkPos centerChunk,
		int chunkRadius
) {
}
