package com.ikunkk02.deathrewind;

import com.ikunkk02.deathrewind.command.DeathRewindCommand;
import com.ikunkk02.deathrewind.config.DeathRewindConfig;
import com.ikunkk02.deathrewind.network.ModNetworking;
import com.ikunkk02.deathrewind.rewind.RewindManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeathRewindMod implements ModInitializer {
	public static final String MOD_ID = "death_rewind";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
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
