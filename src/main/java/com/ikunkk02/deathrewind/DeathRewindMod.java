package com.ikunkk02.deathrewind;

import com.ikunkk02.deathrewind.command.DeathRewindCommand;
import com.ikunkk02.deathrewind.config.DeathRewindConfig;
import com.ikunkk02.deathrewind.network.ModNetworking;
import com.ikunkk02.deathrewind.rewind.RewindManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.api.EnvType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeathRewindMod implements ModInitializer {
	public static final String MOD_ID = "death_rewind";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This mod does not support multiplayer/dedicated servers.
		// The checkpoint system uses static per-player state that breaks with multiple players.
		if (FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER) {
			throw new IllegalStateException(
					"[Death Rewind] 本模组仅支持单人游戏！多人模式/服务器会导致存档系统崩溃。请移除此模组。"
			);
		}

		DeathRewindConfig.load();
		ModSounds.register();
		ModNetworking.registerPayloads();
		RewindManager.register();
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			DeathRewindCommand.register(dispatcher);
		});
		LOGGER.info("Death Rewind initialized.");
	}
}
