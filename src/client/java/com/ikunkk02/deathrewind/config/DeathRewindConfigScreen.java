package com.ikunkk02.deathrewind.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class DeathRewindConfigScreen {
	private DeathRewindConfigScreen() {}

	public static Screen create(Screen parent) {
		DeathRewindConfig config = DeathRewindConfig.get();
		boolean locked = DeathRewindConfig.isLocked();
		String title = locked ? "死亡回溯 - Death Rewind §c[已锁定]" : "死亡回溯 - Death Rewind";

		ConfigBuilder builder = ConfigBuilder.create()
				.setParentScreen(parent)
				.setTitle(Component.literal(title))
				.setSavingRunnable(() -> { if (!locked) DeathRewindConfig.save(); });

		ConfigEntryBuilder entry = builder.entryBuilder();
		ConfigCategory general = builder.getOrCreateCategory(Component.literal("通用"));

		general.addEntry(entry.startIntSlider(
				Component.literal("回溯时长 (秒)"), config.rewindSeconds(), 5, 300)
				.setDefaultValue(60).setTooltip(Component.literal("死亡时回溯多少秒之前的存档点"))
				.setSaveConsumer(config::setRewindSeconds).build());

		general.addEntry(entry.startIntSlider(
				Component.literal("存档点间隔 (秒)"), config.checkpointIntervalSeconds(), 1, 60)
				.setDefaultValue(5).setTooltip(Component.literal("每隔多少秒创建一个存档点"))
				.setSaveConsumer(config::setCheckpointIntervalSeconds).build());

		general.addEntry(entry.startIntSlider(
				Component.literal("最大回溯次数 (极限模式)"), config.maxRewinds(), 0, 99)
				.setDefaultValue(5).setTooltip(Component.literal("仅极限模式生效，生存/创造无限。0 = 无限"))
				.setSaveConsumer(config::setMaxRewinds).build());

		ConfigCategory blocks = builder.getOrCreateCategory(Component.literal("方块回溯"));

		blocks.addEntry(entry.startBooleanToggle(
				Component.literal("启用方块回溯"), config.enableBlockRewind())
				.setDefaultValue(true).setTooltip(Component.literal("破坏/放置的方块是否一起回溯"))
				.setSaveConsumer(config::setEnableBlockRewind).build());

		blocks.addEntry(entry.startIntSlider(
				Component.literal("区块半径"), config.chunkRadius(), 1, 16)
				.setDefaultValue(3).setTooltip(Component.literal("追踪玩家周围多少区块的方块变化"))
				.setSaveConsumer(config::setChunkRadius).build());

		blocks.addEntry(entry.startIntSlider(
				Component.literal("每 tick 最大恢复方块数"), config.maxBlocksRestorePerTick(), 1, 1024)
				.setDefaultValue(128).setTooltip(Component.literal("方块回溯速度，越大越快但可能卡顿"))
				.setSaveConsumer(config::setMaxBlocksRestorePerTick).build());

		ConfigCategory fx = builder.getOrCreateCategory(Component.literal("特效"));

		fx.addEntry(entry.startBooleanToggle(
				Component.literal("启用客户端特效"), config.enableClientEffect())
				.setDefaultValue(true).setTooltip(Component.literal("回溯粒子/画面特效"))
				.setSaveConsumer(config::setEnableClientEffect).build());

		fx.addEntry(entry.startBooleanToggle(
				Component.literal("存档点提示"), config.enableSaveNotification())
				.setDefaultValue(true).setTooltip(Component.literal("右下角显示存档点保存提示"))
				.setSaveConsumer(config::setEnableSaveNotification).build());

		ConfigCategory cooldown = builder.getOrCreateCategory(Component.literal("冷却"));

		cooldown.addEntry(entry.startBooleanToggle(
				Component.literal("启用冷却"), config.enableCooldown())
				.setDefaultValue(false).setTooltip(Component.literal("两次回溯之间需要等待"))
				.setSaveConsumer(config::setEnableCooldown).build());

		cooldown.addEntry(entry.startIntSlider(
				Component.literal("冷却时间 (秒)"), config.cooldownSeconds(), 0, 300)
				.setDefaultValue(0).setTooltip(Component.literal("两次回溯之间的冷却时间"))
				.setSaveConsumer(config::setCooldownSeconds).build());

		return builder.build();
	}
}
