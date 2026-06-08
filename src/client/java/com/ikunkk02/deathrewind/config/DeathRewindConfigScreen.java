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
		String titleKey = locked ? "text.death_rewind.config.title_locked" : "text.death_rewind.config.title";

		ConfigBuilder builder = ConfigBuilder.create()
				.setParentScreen(parent)
				.setTitle(Component.translatable(titleKey))
				.setSavingRunnable(() -> {
					if (!locked) {
						DeathRewindConfig.save();
					}
				});

		ConfigEntryBuilder entry = builder.entryBuilder();
		ConfigCategory general = builder.getOrCreateCategory(
				Component.translatable("text.death_rewind.config.category.general"));

		general.addEntry(entry.startIntSlider(
						Component.translatable("text.death_rewind.config.rewind_duration"),
						config.rewindSeconds(),
						5,
						300
				)
				.setDefaultValue(15)
				.setTooltip(Component.translatable("text.death_rewind.config.rewind_duration.tooltip"))
				.setSaveConsumer(config::setRewindSeconds)
				.build());

		general.addEntry(entry.startIntSlider(
						Component.translatable("text.death_rewind.config.checkpoint_interval"),
						config.checkpointIntervalSeconds(),
						5,
						60
				)
				.setDefaultValue(5)
				.setTooltip(Component.translatable("text.death_rewind.config.checkpoint_interval.tooltip"))
				.setSaveConsumer(config::setCheckpointIntervalSeconds)
				.build());

		general.addEntry(entry.startIntSlider(
						Component.translatable("text.death_rewind.config.max_rewinds"),
						config.maxRewinds(),
						0,
						99
				)
				.setDefaultValue(5)
				.setTooltip(Component.translatable("text.death_rewind.config.max_rewinds.tooltip"))
				.setSaveConsumer(config::setMaxRewinds)
				.build());

		ConfigCategory blocks = builder.getOrCreateCategory(
				Component.translatable("text.death_rewind.config.category.block_rewind"));

		blocks.addEntry(entry.startBooleanToggle(
						Component.translatable("text.death_rewind.config.enable_block_rewind"),
						config.enableBlockRewind()
				)
				.setDefaultValue(true)
				.setTooltip(Component.translatable("text.death_rewind.config.enable_block_rewind.tooltip"))
				.setSaveConsumer(config::setEnableBlockRewind)
				.build());

		blocks.addEntry(entry.startBooleanToggle(
						Component.translatable("text.death_rewind.config.optimization_mod_compatibility"),
						config.optimizationModCompatibility()
				)
				.setDefaultValue(true)
				.setTooltip(Component.translatable("text.death_rewind.config.optimization_mod_compatibility.tooltip"))
				.setSaveConsumer(config::setOptimizationModCompatibility)
				.build());

		blocks.addEntry(entry.startIntSlider(
						Component.translatable("text.death_rewind.config.chunk_radius"),
						config.chunkRadius(),
						1,
						16
				)
				.setDefaultValue(3)
				.setTooltip(Component.translatable("text.death_rewind.config.chunk_radius.tooltip"))
				.setSaveConsumer(config::setChunkRadius)
				.build());

		blocks.addEntry(entry.startIntSlider(
						Component.translatable("text.death_rewind.config.max_blocks_per_tick"),
						config.maxBlocksRestorePerTick(),
						1,
						1024
				)
				.setDefaultValue(128)
				.setTooltip(Component.translatable("text.death_rewind.config.max_blocks_per_tick.tooltip"))
				.setSaveConsumer(config::setMaxBlocksRestorePerTick)
				.build());

		ConfigCategory effects = builder.getOrCreateCategory(
				Component.translatable("text.death_rewind.config.category.effects"));

		effects.addEntry(entry.startBooleanToggle(
						Component.translatable("text.death_rewind.config.enable_client_effects"),
						config.enableClientEffect()
				)
				.setDefaultValue(true)
				.setTooltip(Component.translatable("text.death_rewind.config.enable_client_effects.tooltip"))
				.setSaveConsumer(config::setEnableClientEffect)
				.build());

		effects.addEntry(entry.startBooleanToggle(
						Component.translatable("text.death_rewind.config.checkpoint_notification"),
						config.enableSaveNotification()
				)
				.setDefaultValue(true)
				.setTooltip(Component.translatable("text.death_rewind.config.checkpoint_notification.tooltip"))
				.setSaveConsumer(config::setEnableSaveNotification)
				.build());

		ConfigCategory cooldown = builder.getOrCreateCategory(
				Component.translatable("text.death_rewind.config.category.cooldown"));

		cooldown.addEntry(entry.startBooleanToggle(
						Component.translatable("text.death_rewind.config.enable_cooldown"),
						config.enableCooldown()
				)
				.setDefaultValue(false)
				.setTooltip(Component.translatable("text.death_rewind.config.enable_cooldown.tooltip"))
				.setSaveConsumer(config::setEnableCooldown)
				.build());

		cooldown.addEntry(entry.startIntSlider(
						Component.translatable("text.death_rewind.config.cooldown_duration"),
						config.cooldownSeconds(),
						0,
						300
				)
				.setDefaultValue(0)
				.setTooltip(Component.translatable("text.death_rewind.config.cooldown_duration.tooltip"))
				.setSaveConsumer(config::setCooldownSeconds)
				.build());

		return builder.build();
	}
}
