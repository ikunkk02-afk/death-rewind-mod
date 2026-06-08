package com.ikunkk02.deathrewind.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ikunkk02.deathrewind.DeathRewindMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class DeathRewindConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final String FILE_NAME = "death_rewind.json";
	private static DeathRewindConfig instance = defaults();
	private static boolean locked;
	private static int lockedMaxRewinds;
	private static int lockedRewindSeconds;

	private int rewindSeconds = 15;
	private int checkpointIntervalSeconds = 5;
	private int maxRewinds = 5;
	private int chunkRadius = 3;
	private int invulnerableTicks = 60;
	private int maxBlocksRestorePerTick = 128;
	private boolean enableBlockRewind = true;
	private Boolean optimizationModCompatibility = true;
	private boolean enableClientEffect = true;
	private boolean enableCooldown = false;
	private boolean enableSaveNotification = true;
	private int cooldownSeconds = 0;

	public static DeathRewindConfig get() {
		return instance;
	}

	/** Lock current config values for hardcore worlds. Changes won't apply until a new world. */
	public static void lockForHardcore() {
		if (!locked) {
			locked = true;
			lockedMaxRewinds = instance.maxRewinds;
			lockedRewindSeconds = instance.rewindSeconds;
		}
	}

	public static boolean isLocked() {
		return locked;
	}

	public static void unlock() {
		locked = false;
	}

	public int effectiveMaxRewinds() {
		return locked ? lockedMaxRewinds : maxRewinds;
	}

	public int effectiveRewindSeconds() {
		return locked ? lockedRewindSeconds : rewindSeconds;
	}

	public static void load() {
		Path path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);

		try {
			Files.createDirectories(path.getParent());

			if (Files.notExists(path)) {
				instance = defaults();
				save(path, instance);
				return;
			}

			try (Reader reader = Files.newBufferedReader(path)) {
				DeathRewindConfig loaded = GSON.fromJson(reader, DeathRewindConfig.class);
				instance = loaded == null ? defaults() : loaded.sanitized();
			}

			save(path, instance);
		} catch (IOException | RuntimeException ex) {
			DeathRewindMod.LOGGER.warn("Failed to load Death Rewind config, using defaults.", ex);
			instance = defaults();
		}
	}

	private static void save(Path path, DeathRewindConfig config) throws IOException {
		try (Writer writer = Files.newBufferedWriter(path)) {
			GSON.toJson(config, writer);
		}
	}

	private static DeathRewindConfig defaults() {
		return new DeathRewindConfig();
	}

	private DeathRewindConfig sanitized() {
		rewindSeconds = Math.max(1, rewindSeconds);
		checkpointIntervalSeconds = Math.max(5, checkpointIntervalSeconds);
		maxRewinds = Math.max(0, maxRewinds);
		chunkRadius = Math.max(1, chunkRadius);
		invulnerableTicks = Math.max(1, invulnerableTicks);
		maxBlocksRestorePerTick = Math.max(1, maxBlocksRestorePerTick);
		if (optimizationModCompatibility == null) {
			optimizationModCompatibility = true;
		}
		cooldownSeconds = Math.max(0, cooldownSeconds);
		return this;
	}

	public int rewindSeconds() {
		return rewindSeconds;
	}

	public int rewindTicks() {
		return rewindSeconds * 20;
	}

	public int checkpointIntervalSeconds() {
		return checkpointIntervalSeconds;
	}

	public int checkpointIntervalTicks() {
		return checkpointIntervalSeconds * 20;
	}

	public void setCheckpointIntervalSeconds(int seconds) {
		checkpointIntervalSeconds = Math.max(5, seconds);
	}

	public int maxRewinds() {
		return maxRewinds;
	}

	public void setMaxRewinds(int count) {
		maxRewinds = Math.max(0, count);
	}

	public void setRewindSeconds(int seconds) {
		rewindSeconds = Math.max(1, seconds);
	}

	public void setInvulnerableTicks(int ticks) {
		invulnerableTicks = Math.max(0, ticks);
	}

	public void setEnableBlockRewind(boolean enable) {
		enableBlockRewind = enable;
	}

	public void setOptimizationModCompatibility(boolean enable) {
		optimizationModCompatibility = enable;
	}

	public void setEnableClientEffect(boolean enable) {
		enableClientEffect = enable;
	}

	public void setEnableCooldown(boolean enable) {
		enableCooldown = enable;
	}

	public void setCooldownSeconds(int seconds) {
		cooldownSeconds = Math.max(0, seconds);
	}

	public void setChunkRadius(int radius) {
		chunkRadius = Math.max(1, radius);
	}

	public void setMaxBlocksRestorePerTick(int count) {
		maxBlocksRestorePerTick = Math.max(1, count);
	}

	public static void save() {
		Path path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
		try {
			Files.createDirectories(path.getParent());
			try (Writer writer = Files.newBufferedWriter(path)) {
				GSON.toJson(instance.sanitized(), writer);
			}
		} catch (IOException e) {
			DeathRewindMod.LOGGER.warn("Failed to save Death Rewind config.", e);
		}
	}

	public int cooldownSeconds() {
		return cooldownSeconds;
	}

	public int invulnerableTicks() {
		return invulnerableTicks;
	}

	public boolean enableCooldown() {
		return enableCooldown;
	}

	public int cooldownTicks() {
		return cooldownSeconds * 20;
	}

	public boolean enableBlockRewind() {
		return enableBlockRewind;
	}

	public boolean optimizationModCompatibility() {
		return optimizationModCompatibility == null || optimizationModCompatibility;
	}

	public boolean enableClientEffect() {
		return enableClientEffect;
	}

	public boolean enableSaveNotification() {
		return enableSaveNotification;
	}

	public void setEnableSaveNotification(boolean enable) {
		enableSaveNotification = enable;
	}

	public int chunkRadius() {
		return chunkRadius;
	}

	public int maxBlocksRestorePerTick() {
		return maxBlocksRestorePerTick;
	}
}
