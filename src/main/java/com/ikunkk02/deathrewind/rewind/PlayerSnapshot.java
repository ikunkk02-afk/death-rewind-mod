package com.ikunkk02.deathrewind.rewind;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public record PlayerSnapshot(
		long gameTime,
		ResourceKey<Level> dimension,
		double x,
		double y,
		double z,
		float yaw,
		float pitch,
		float health,
		int foodLevel,
		float saturationLevel,
		int experienceLevel,
		float experienceProgress,
		int totalExperience,
		int selectedSlot,
		List<ItemStack> inventoryItems,
		List<MobEffectInstance> effects,
		int remainingFireTicks,
		double fallDistance,
		int rewindGeneration
) {
	public Vec3 position() {
		return new Vec3(x, y, z);
	}

	public BlockPos blockPos() {
		return BlockPos.containing(x, y, z);
	}

	public static PlayerSnapshot capture(ServerPlayer player, long gameTime, int rewindGeneration) {
		Inventory inventory = player.getInventory();
		List<ItemStack> inventoryItems = new ArrayList<>(inventory.getContainerSize());

		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			inventoryItems.add(copyStack(inventory.getItem(slot)));
		}

		Collection<MobEffectInstance> activeEffects = player.getActiveEffects();
		List<MobEffectInstance> effects = new ArrayList<>(activeEffects.size());

		for (MobEffectInstance effect : activeEffects) {
			effects.add(new MobEffectInstance(effect));
		}

		FoodData foodData = player.getFoodData();
		return new PlayerSnapshot(
				gameTime,
				player.level().dimension(),
				player.getX(),
				player.getY(),
				player.getZ(),
				player.getYRot(),
				player.getXRot(),
				player.getHealth(),
				foodData.getFoodLevel(),
				foodData.getSaturationLevel(),
				player.experienceLevel,
				player.experienceProgress,
				player.totalExperience,
				inventory.getSelectedSlot(),
				List.copyOf(inventoryItems),
				List.copyOf(effects),
				player.getRemainingFireTicks(),
				player.fallDistance,
				rewindGeneration
		);
	}

	public boolean restore(ServerPlayer player) {
		ServerLevel targetLevel = player.level().getServer().getLevel(dimension);

		if (targetLevel == null) {
			return false;
		}

		player.closeContainer();
		boolean teleported = player.teleportTo(targetLevel, x, y, z, Set.<Relative>of(), yaw, pitch, false);
		if (!teleported) {
			return false;
		}

		player.setDeltaMovement(Vec3.ZERO);
		player.setYRot(yaw);
		player.setXRot(pitch);
		player.setHealth(clamp(health, 1.0F, player.getMaxHealth()));

		restoreFood(player);
		restoreExperience(player);
		restoreInventory(player);
		restoreEffects(player);

		player.setRemainingFireTicks(remainingFireTicks);
		player.fallDistance = fallDistance;
		player.resetSentInfo();
		player.containerMenu.broadcastChanges();
		player.inventoryMenu.broadcastChanges();
		return true;
	}

	private void restoreFood(ServerPlayer player) {
		FoodData foodData = player.getFoodData();
		foodData.setFoodLevel(foodLevel);
		foodData.setSaturation(saturationLevel);
	}

	private void restoreExperience(ServerPlayer player) {
		player.experienceLevel = Math.max(0, experienceLevel);
		player.experienceProgress = clamp(experienceProgress, 0.0F, 1.0F);
		player.totalExperience = Math.max(0, totalExperience);
	}

	private void restoreInventory(ServerPlayer player) {
		Inventory inventory = player.getInventory();
		inventory.clearContent();

		for (int slot = 0; slot < inventory.getContainerSize() && slot < inventoryItems.size(); slot++) {
			inventory.setItem(slot, copyStack(inventoryItems.get(slot)));
		}

		int maxSelectedSlot = Math.max(0, Inventory.getSelectionSize() - 1);
		inventory.setSelectedSlot(Math.max(0, Math.min(selectedSlot, maxSelectedSlot)));
		inventory.setChanged();
	}

	private void restoreEffects(ServerPlayer player) {
		player.removeAllEffects();

		for (MobEffectInstance effect : effects) {
			player.addEffect(new MobEffectInstance(effect));
		}
	}

	private static ItemStack copyStack(ItemStack stack) {
		return stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
	}

	private static float clamp(float value, float min, float max) {
		return Math.max(min, Math.min(max, value));
	}
}
