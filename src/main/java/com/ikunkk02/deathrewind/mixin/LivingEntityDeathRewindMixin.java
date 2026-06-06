package com.ikunkk02.deathrewind.mixin;

import com.ikunkk02.deathrewind.rewind.RewindManager;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public abstract class LivingEntityDeathRewindMixin {
	@WrapOperation(
			method = "hurtServer",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/entity/LivingEntity;checkTotemDeathProtection(Lnet/minecraft/world/damagesource/DamageSource;)Z"
			)
	)
	private boolean deathRewind$rewindBeforeDeath(LivingEntity instance, DamageSource source, Operation<Boolean> original) {
		if (original.call(instance, source)) {
			return true;
		}

		if (instance instanceof ServerPlayer player) {
			return RewindManager.tryStartDeathRewind(player, source);
		}
		return false;
	}
}
