package com.ikunkk02.deathrewind;

import com.ikunkk02.deathrewind.command.DeathRewindCommand;
import com.ikunkk02.deathrewind.config.DeathRewindConfig;
import com.ikunkk02.deathrewind.network.ModNetworking;
import com.ikunkk02.deathrewind.rewind.RewindManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeathRewindMod implements ModInitializer {
	public static final String MOD_ID = "death_rewind";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	private static boolean c2meDetected;
	private static boolean c2meDisabledBlockRewind;

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	public static boolean isC2meDetected() {
		return c2meDetected;
	}

	public static boolean c2meDisabledBlockRewind() {
		return c2meDisabledBlockRewind;
	}

	@Override
	public void onInitialize() {
		// This mod does not support multiplayer/dedicated servers.
		// The checkpoint system uses static per-player state that breaks with multiple players.
		if (FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER) {
			throw new IllegalStateException(
					Component.translatable("text.death_rewind.server_rejected").getString()
			);
		}

		DeathRewindConfig.load();
		applyOptimizationModSafety();
		ModSounds.register();
		ModNetworking.registerPayloads();
		RewindManager.register();
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				DeathRewindCommand.register(dispatcher)
		);
		LOGGER.info("Death Rewind initialized.");
	}

	private static void applyOptimizationModSafety() {
		c2meDetected = FabricLoader.getInstance().isModLoaded("c2me");
		c2meDisabledBlockRewind = false;

		if (!c2meDetected) {
			return;
		}

		DeathRewindConfig config = DeathRewindConfig.get();
		if (config.optimizationModCompatibility()) {
			if (config.enableBlockRewind()) {
				config.setEnableBlockRewind(false);
				DeathRewindConfig.save();
			}
			c2meDisabledBlockRewind = true;
			LOGGER.warn("C2ME detected. Block rewind has been disabled automatically for stability. Disable Optimization Mod Compatibility and re-enable Block Rewind if you want to force it.");
		} else {
			LOGGER.warn("C2ME detected. Death Rewind does not officially support C2ME block rewind compatibility.");
		}
	}
}
