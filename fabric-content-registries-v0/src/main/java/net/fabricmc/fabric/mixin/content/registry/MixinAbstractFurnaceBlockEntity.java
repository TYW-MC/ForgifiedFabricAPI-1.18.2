/*
 * Copyright (c) 2016, 2017, 2018, 2019 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.fabricmc.fabric.mixin.content.registry;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeType;

import net.minecraftforge.common.ForgeHooks;

import net.fabricmc.fabric.api.registry.FuelRegistry;
import net.fabricmc.fabric.impl.content.registry.FuelRegistryImpl;

@Mixin(AbstractFurnaceBlockEntity.class)
public class MixinAbstractFurnaceBlockEntity {
	@Inject(at = @At("RETURN"), method = "createFuelTimeMap")
	private static void fuelTimeMapHook(CallbackInfoReturnable<Map<Item, Integer>> info) {
		((FuelRegistryImpl) FuelRegistry.INSTANCE).apply(info.getReturnValue());
	}

	// 1.18.2 note: forge's binpatch replaces the vanilla createFuelTimeMap() lookups in canUseAsFuel
	// and getFuelTime with ForgeHooks.getBurnTime, so both redirects target that call instead. The
	// fabric fuel registry still wins; anything it does not know about is delegated to forge.
	@Redirect(method = "canUseAsFuel", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/common/ForgeHooks;getBurnTime(Lnet/minecraft/item/ItemStack;Lnet/minecraft/recipe/RecipeType;)I"))
	private static int canUseAsFuelRedirect(ItemStack stack, RecipeType<?> type) {
		return fabricFuelTime(stack, type);
	}

	@Redirect(method = "getFuelTime", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/common/ForgeHooks;getBurnTime(Lnet/minecraft/item/ItemStack;Lnet/minecraft/recipe/RecipeType;)I"))
	private int getFuelTimeRedirect(ItemStack stack, RecipeType<?> type) {
		return fabricFuelTime(stack, type);
	}

	private static int fabricFuelTime(ItemStack stack, RecipeType<?> type) {
		Integer time = ((FuelRegistryImpl) FuelRegistry.INSTANCE).getFuelTimes().get(stack.getItem());
		return time != null ? time : ForgeHooks.getBurnTime(stack, type);
	}
}
