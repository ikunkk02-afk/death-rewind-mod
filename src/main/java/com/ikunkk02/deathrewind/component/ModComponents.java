package com.ikunkk02.deathrewind.component;

import com.ikunkk02.deathrewind.DeathRewindMod;
import net.minecraft.resources.Identifier;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.entity.EntityComponentFactoryRegistry;
import org.ladysnake.cca.api.v3.entity.EntityComponentInitializer;
import org.ladysnake.cca.api.v3.entity.RespawnCopyStrategy;

public final class ModComponents implements EntityComponentInitializer {
	public static final ComponentKey<PlayerRewindComponent> PLAYER_REWIND =
			ComponentRegistry.getOrCreate(
					Identifier.fromNamespaceAndPath(DeathRewindMod.MOD_ID, "player_rewind"),
					PlayerRewindComponent.class
			);

	@Override
	public void registerEntityComponentFactories(EntityComponentFactoryRegistry registry) {
		registry.registerForPlayers(PLAYER_REWIND, PlayerRewindComponentImpl::new, RespawnCopyStrategy.ALWAYS_COPY);
	}
}
