package com.ikunkk02.deathrewind.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public final class ModNetworking {
	private static boolean registered;

	private ModNetworking() {
	}

	public static void registerPayloads() {
		if (registered) {
			return;
		}

		registered = true;
		PayloadTypeRegistry.clientboundPlay().register(RewindStartPayload.TYPE, RewindStartPayload.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(RewindEndPayload.TYPE, RewindEndPayload.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(RewindBlockParticlePayload.TYPE, RewindBlockParticlePayload.STREAM_CODEC);
	}

	public static void sendRewindStart(ServerPlayer player, int durationTicks, Vec3 startPos, Vec3 endPos) {
		send(player, new RewindStartPayload(player.getUUID(), durationTicks, startPos, endPos));
	}

	public static void sendRewindEnd(ServerPlayer player, UUID playerUuid) {
		send(player, new RewindEndPayload(playerUuid));
	}

	public static void sendBlockParticle(ServerLevel level, BlockPos pos, Identifier particleType) {
		RewindBlockParticlePayload payload = new RewindBlockParticlePayload(pos.immutable(), particleType, level.dimension());

		for (ServerPlayer player : level.players()) {
			send(player, payload);
		}
	}

	private static void send(ServerPlayer player, CustomPacketPayload payload) {
		if (ServerPlayNetworking.canSend(player, payload.type())) {
			ServerPlayNetworking.send(player, payload);
		}
	}
}
