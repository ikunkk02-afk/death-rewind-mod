package com.ikunkk02.deathrewind.network;

import com.ikunkk02.deathrewind.DeathRewindMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public record RewindBlockParticlePayload(
		BlockPos pos,
		Identifier particleType,
		ResourceKey<Level> dimension
) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<RewindBlockParticlePayload> TYPE =
			new CustomPacketPayload.Type<>(DeathRewindMod.id("rewind_block_particle"));
	public static final StreamCodec<RegistryFriendlyByteBuf, RewindBlockParticlePayload> STREAM_CODEC =
			StreamCodec.of((buf, payload) -> payload.write(buf), RewindBlockParticlePayload::read);

	private void write(RegistryFriendlyByteBuf buf) {
		buf.writeBlockPos(pos);
		buf.writeIdentifier(particleType);
		ResourceKey.streamCodec(Registries.DIMENSION).encode(buf, dimension);
	}

	private static RewindBlockParticlePayload read(RegistryFriendlyByteBuf buf) {
		return new RewindBlockParticlePayload(
				buf.readBlockPos(),
				buf.readIdentifier(),
				ResourceKey.streamCodec(Registries.DIMENSION).decode(buf)
		);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
