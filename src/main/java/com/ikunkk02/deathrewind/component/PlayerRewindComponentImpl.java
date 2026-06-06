package com.ikunkk02.deathrewind.component;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class PlayerRewindComponentImpl implements PlayerRewindComponent {
	private boolean rewinding;
	private int cooldownTicks;
	private long lastRewindGameTime;
	private BlockPos lastSafePos;
	private int rewindCount;

	public PlayerRewindComponentImpl(Player player) {
	}

	@Override
	public boolean isRewinding() {
		return rewinding;
	}

	@Override
	public void setRewinding(boolean rewinding) {
		this.rewinding = rewinding;
	}

	@Override
	public int cooldownTicks() {
		return cooldownTicks;
	}

	@Override
	public void setCooldownTicks(int cooldownTicks) {
		this.cooldownTicks = Math.max(0, cooldownTicks);
	}

	@Override
	public void tickCooldown() {
		if (cooldownTicks > 0) {
			cooldownTicks--;
		}
	}

	@Override
	public long lastRewindGameTime() {
		return lastRewindGameTime;
	}

	@Override
	public void setLastRewindGameTime(long lastRewindGameTime) {
		this.lastRewindGameTime = lastRewindGameTime;
	}

	@Override
	public BlockPos lastSafePos() {
		return lastSafePos;
	}

	@Override
	public void setLastSafePos(BlockPos lastSafePos) {
		this.lastSafePos = lastSafePos;
	}

	@Override
	public int rewindCount() {
		return rewindCount;
	}

	@Override
	public void incrementRewindCount() {
		rewindCount++;
	}

	@Override
	public void setRewindCount(int count) {
		rewindCount = Math.max(0, count);
	}

	@Override
	public void readData(ValueInput input) {
		rewinding = input.getBooleanOr("Rewinding", false);
		cooldownTicks = input.getIntOr("CooldownTicks", 0);
		lastRewindGameTime = input.getLongOr("LastRewindGameTime", 0L);
		rewindCount = input.getIntOr("RewindCount", 0);

		if (input.getBooleanOr("HasLastSafePos", false)) {
			lastSafePos = BlockPos.of(input.getLongOr("LastSafePos", 0L));
		} else {
			lastSafePos = null;
		}
	}

	@Override
	public void writeData(ValueOutput output) {
		output.putBoolean("Rewinding", rewinding);
		output.putInt("CooldownTicks", cooldownTicks);
		output.putLong("LastRewindGameTime", lastRewindGameTime);
		output.putInt("RewindCount", rewindCount);
		output.putBoolean("HasLastSafePos", lastSafePos != null);

		if (lastSafePos != null) {
			output.putLong("LastSafePos", lastSafePos.asLong());
		}
	}
}
