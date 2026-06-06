package com.ikunkk02.deathrewind.rewind;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

public record BlockChangeRecord(
		long gameTime,
		ResourceKey<Level> dimension,
		BlockPos pos,
		BlockState previousState,
		@Nullable CompoundTag previousBlockEntityNbt,
		UUID playerUuid,
		int timelineId,
		int blockChangeLogIndex
) {
	public BlockChangeRecord {
		pos = pos.immutable();
		previousBlockEntityNbt = previousBlockEntityNbt == null ? null : previousBlockEntityNbt.copy();
	}
}
