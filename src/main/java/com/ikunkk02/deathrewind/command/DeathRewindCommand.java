package com.ikunkk02.deathrewind.command;

import com.ikunkk02.deathrewind.component.ModComponents;
import com.ikunkk02.deathrewind.component.PlayerRewindComponent;
import com.ikunkk02.deathrewind.config.DeathRewindConfig;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class DeathRewindCommand {
	private DeathRewindCommand() {}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("deathrewind")
				.executes(ctx -> showStatus(ctx.getSource()))
				.then(Commands.literal("set")
						.then(Commands.argument("count", IntegerArgumentType.integer(0, 999))
								.executes(ctx -> setMaxRewinds(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "count")))
						)
				)
				.then(Commands.literal("reload")
						.executes(ctx -> reloadConfig(ctx.getSource()))
				)
		);
	}

	private static int showStatus(CommandSourceStack source) {
		ServerPlayer player = source.getPlayer();
		if (player == null) {
			source.sendFailure(Component.literal("This command can only be used by a player."));
			return 0;
		}

		DeathRewindConfig config = DeathRewindConfig.get();
		PlayerRewindComponent comp = ModComponents.PLAYER_REWIND.get(player);
		int used = comp.rewindCount();
		int max = config.maxRewinds();
		boolean isHardcore = player.level().getLevelData().isHardcore();
		String limitStr = (!isHardcore || max == 0) ? "无限 (非极限模式)" : String.format("%d/%d (剩余 %d)", used, max, Math.max(0, max - used));

		source.sendSuccess(() -> Component.literal(
				String.format("§6=== 死亡回溯 §r===\n  §7次数: §e%s§7  回溯: §b%d秒§7  无敌: §e10秒",
						limitStr, config.rewindSeconds())
		), false);

		return 1;
	}

	private static int setMaxRewinds(CommandSourceStack source, int count) {
		DeathRewindConfig config = DeathRewindConfig.get();
		config.setMaxRewinds(count);
		source.sendSuccess(() -> Component.literal(
				String.format("§a回溯次数上限已设为 %d§r", count)
		), true);
		return 1;
	}

	private static int reloadConfig(CommandSourceStack source) {
		DeathRewindConfig.load();
		source.sendSuccess(() -> Component.literal("§a死亡回溯配置已重载§r"), true);
		return 1;
	}
}
