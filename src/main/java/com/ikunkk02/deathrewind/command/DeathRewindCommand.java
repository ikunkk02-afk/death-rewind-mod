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
	private DeathRewindCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("deathrewind")
				.executes(ctx -> showStatus(ctx.getSource()))
				.then(Commands.literal("set")
						.then(Commands.argument("count", IntegerArgumentType.integer(0, 999))
								.executes(ctx -> setMaxRewinds(
										ctx.getSource(),
										IntegerArgumentType.getInteger(ctx, "count")
								))
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
			source.sendFailure(Component.translatable("text.death_rewind.cmd.player_only"));
			return 0;
		}

		DeathRewindConfig config = DeathRewindConfig.get();
		PlayerRewindComponent component = ModComponents.PLAYER_REWIND.get(player);
		int used = component.rewindCount();
		int max = config.maxRewinds();
		boolean isHardcore = player.level().getLevelData().isHardcore();
		String limit = (!isHardcore || max == 0)
				? Component.translatable("text.death_rewind.cmd.unlimited").getString()
				: Component.translatable("text.death_rewind.cmd.used_format", used, max, Math.max(0, max - used)).getString();

		source.sendSuccess(() -> Component.translatable("text.death_rewind.cmd.status",
				limit,
				config.rewindSeconds(),
				config.invulnerableTicks() / 20.0F
		), false);

		return 1;
	}

	private static int setMaxRewinds(CommandSourceStack source, int count) {
		DeathRewindConfig config = DeathRewindConfig.get();
		config.setMaxRewinds(count);
		source.sendSuccess(() -> Component.translatable("text.death_rewind.cmd.set", count), true);
		return 1;
	}

	private static int reloadConfig(CommandSourceStack source) {
		DeathRewindConfig.load();
		source.sendSuccess(() -> Component.translatable("text.death_rewind.cmd.reloaded"), true);
		return 1;
	}
}
