package com.ikunkk02.deathrewind.rewind;

import com.ikunkk02.deathrewind.config.DeathRewindConfig;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.UUID;

final class RewindExecutor {
	private final UUID playerId;
	private final ResourceKey<Level> dimension;
	private final Deque<BlockChangeRecord> queue;

	RewindExecutor(UUID playerId, ResourceKey<Level> dimension, Collection<BlockChangeRecord> records) {
		this.playerId = playerId;
		this.dimension = dimension;
		this.queue = new ArrayDeque<>(records);
	}

	UUID playerId() {
		return playerId;
	}

	boolean tick(MinecraftServer server, DeathRewindConfig config) {
		ServerLevel level = server.getLevel(dimension);
		if (level == null) {
			queue.clear();
			return true;
		}

		int restored = 0;
		int maxPerTick = Math.max(1, config.maxBlocksRestorePerTick());

		while (restored < maxPerTick && !queue.isEmpty()) {
			BlockRewindManager.restoreRecord(level, queue.removeFirst());
			restored++;
		}

		return queue.isEmpty();
	}
}
