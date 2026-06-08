package com.ikunkk02.deathrewind.network;

import com.ikunkk02.deathrewind.DeathRewindMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record RewindEndPayload(UUID playerUuid) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<RewindEndPayload> TYPE =
			new CustomPacketPayload.Type<>(DeathRewindMod.id("rewind_end"));
	public static final StreamCodec<RegistryFriendlyByteBuf, RewindEndPayload> STREAM_CODEC =
			StreamCodec.of((buf, payload) -> payload.write(buf), RewindEndPayload::read);

	private void write(RegistryFriendlyByteBuf buf) {
		buf.writeUUID(playerUuid);
	}

	private static RewindEndPayload read(RegistryFriendlyByteBuf buf) {
		return new RewindEndPayload(buf.readUUID());
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
