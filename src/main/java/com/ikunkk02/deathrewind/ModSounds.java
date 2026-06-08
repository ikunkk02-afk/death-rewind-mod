package com.ikunkk02.deathrewind;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public final class ModSounds {
	private static final Identifier ID = DeathRewindMod.id("rewind_bell");
	public static final SoundEvent REWIND_BELL = SoundEvent.createVariableRangeEvent(ID);

	private ModSounds() {
	}

	public static void register() {
		Registry.register(BuiltInRegistries.SOUND_EVENT, ID, REWIND_BELL);
	}
}
