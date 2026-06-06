package com.ikunkk02.deathrewind.component;

import net.minecraft.core.BlockPos;
import org.ladysnake.cca.api.v3.component.ComponentV3;

public interface PlayerRewindComponent extends ComponentV3 {
	boolean isRewinding();

	void setRewinding(boolean rewinding);

	int cooldownTicks();

	void setCooldownTicks(int cooldownTicks);

	void tickCooldown();

	long lastRewindGameTime();

	void setLastRewindGameTime(long lastRewindGameTime);

	BlockPos lastSafePos();

	void setLastSafePos(BlockPos lastSafePos);

	int rewindCount();

	void incrementRewindCount();

	void setRewindCount(int count);
}
