package com.ikunkk02.deathrewind.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class DeathRewindConfigScreen {
	private DeathRewindConfigScreen() {
	}

	public static Screen create(Screen parent) {
		DeathRewindConfig config = DeathRewindConfig.get();
		boolean locked = DeathRewindConfig.isLocked();
		String title = locked ? "死亡回溯 [已锁定]" : "死亡回溯";

		ConfigBuilder builder = ConfigBuilder.create()
				.setParentScreen(parent)
				.setTitle(Component.literal(title))
				.setSavingRunnable(() -> {
					if (!locked) {
						DeathRewindConfig.save();
					}
				});

		ConfigEntryBuilder entry = builder.entryBuilder();
		ConfigCategory general = builder.getOrCreateCategory(Component.literal("General"));

		general.addEntry(entry.startIntSlider(
						Component.literal("Rewind Duration (seconds)"),
						config.rewindSeconds(),
						5,
						300
				)
				.setDefaultValue(15)
				.setTooltip(Component.literal("How far back the player rewinds after death."))
				.setSaveConsumer(config::setRewindSeconds)
				.build());

		general.addEntry(entry.startIntSlider(
						Component.literal("Checkpoint Interval (seconds)"),
						config.checkpointIntervalSeconds(),
						1,
						60
				)
				.setDefaultValue(5)
				.setTooltip(Component.literal("How often a new checkpoint is created."))
				.setSaveConsumer(config::setCheckpointIntervalSeconds)
				.build());

		general.addEntry(entry.startIntSlider(
						Component.literal("Max Rewinds (hardcore)"),
						config.maxRewinds(),
						0,
						99
				)
				.setDefaultValue(5)
				.setTooltip(Component.literal("Only applies in hardcore worlds. 0 means unlimited."))
				.setSaveConsumer(config::setMaxRewinds)
				.build());

		ConfigCategory blocks = builder.getOrCreateCategory(Component.literal("Block Rewind"));

		blocks.addEntry(entry.startBooleanToggle(
						Component.literal("Enable Block Rewind"),
						config.enableBlockRewind()
				)
				.setDefaultValue(true)
				.setTooltip(Component.literal("Restore changed blocks near the player during rewind."))
				.setSaveConsumer(config::setEnableBlockRewind)
				.build());

		blocks.addEntry(entry.startIntSlider(
						Component.literal("Chunk Radius"),
						config.chunkRadius(),
						1,
						16
				)
				.setDefaultValue(3)
				.setTooltip(Component.literal("How many chunks around the player are tracked."))
				.setSaveConsumer(config::setChunkRadius)
				.build());

		blocks.addEntry(entry.startIntSlider(
						Component.literal("Max Blocks Restored Per Tick"),
						config.maxBlocksRestorePerTick(),
						1,
						1024
				)
				.setDefaultValue(128)
				.setTooltip(Component.literal("Higher values restore faster but may cause lag."))
				.setSaveConsumer(config::setMaxBlocksRestorePerTick)
				.build());

		ConfigCategory effects = builder.getOrCreateCategory(Component.literal("Effects"));

		effects.addEntry(entry.startBooleanToggle(
						Component.literal("Enable Client Effects"),
						config.enableClientEffect()
				)
				.setDefaultValue(true)
				.setTooltip(Component.literal("Show rewind particles and screen effects."))
				.setSaveConsumer(config::setEnableClientEffect)
				.build());

		effects.addEntry(entry.startBooleanToggle(
						Component.literal("Checkpoint Notification"),
						config.enableSaveNotification()
				)
				.setDefaultValue(true)
				.setTooltip(Component.literal("Show an action-bar message when checkpoints are saved."))
				.setSaveConsumer(config::setEnableSaveNotification)
				.build());

		ConfigCategory cooldown = builder.getOrCreateCategory(Component.literal("Cooldown"));

		cooldown.addEntry(entry.startBooleanToggle(
						Component.literal("Enable Cooldown"),
						config.enableCooldown()
				)
				.setDefaultValue(false)
				.setTooltip(Component.literal("Require a delay between rewinds."))
				.setSaveConsumer(config::setEnableCooldown)
				.build());

		cooldown.addEntry(entry.startIntSlider(
						Component.literal("Cooldown Duration (seconds)"),
						config.cooldownSeconds(),
						0,
						300
				)
				.setDefaultValue(0)
				.setTooltip(Component.literal("Delay between two rewinds."))
				.setSaveConsumer(config::setCooldownSeconds)
				.build());

		return builder.build();
	}
}
