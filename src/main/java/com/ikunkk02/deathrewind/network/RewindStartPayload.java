package com.ikunkk02.deathrewind.network;

import com.ikunkk02.deathrewind.DeathRewindMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public record RewindStartPayload(UUID playerUuid, int durationTicks, Vec3 startPos, Vec3 endPos) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<RewindStartPayload> TYPE =
			new CustomPacketPayload.Type<>(DeathRewindMod.id("rewind_start"));
	public static final StreamCodec<RegistryFriendlyByteBuf, RewindStartPayload> STREAM_CODEC =
			StreamCodec.of((buf, payload) -> payload.write(buf), RewindStartPayload::read);

	private void write(RegistryFriendlyByteBuf buf) {
		buf.writeUUID(playerUuid);
		buf.writeVarInt(durationTicks);
		Vec3.STREAM_CODEC.encode(buf, startPos);
		Vec3.STREAM_CODEC.encode(buf, endPos);
	}

	private static RewindStartPayload read(RegistryFriendlyByteBuf buf) {
		return new RewindStartPayload(
				buf.readUUID(),
				buf.readVarInt(),
				Vec3.STREAM_CODEC.decode(buf),
				Vec3.STREAM_CODEC.decode(buf)
		);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
