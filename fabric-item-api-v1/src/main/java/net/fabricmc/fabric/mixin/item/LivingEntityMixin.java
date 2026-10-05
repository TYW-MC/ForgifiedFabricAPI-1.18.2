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

package net.fabricmc.fabric.mixin.item;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import net.fabricmc.fabric.api.item.v1.EquipmentSlotProvider;
import net.fabricmc.fabric.impl.item.ItemExtensions;

@Mixin(LivingEntity.class)
abstract class LivingEntityMixin {
	@Inject(method = "getPreferredEquipmentSlot", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;getItem()Lnet/minecraft/item/Item;"), cancellable = true)
	private static void onGetPreferredEquipmentSlot(ItemStack stack, CallbackInfoReturnable<EquipmentSlot> info) {
		// 1.18.2 note: INVOKE_ASSIGN return capture is not applied by this mixin stack, so the
		// item is read from the stack argument instead (getItem is a pure accessor here).
		Item item = stack.getItem();
		EquipmentSlotProvider equipmentSlotProvider = ((ItemExtensions) item).fabric_getEquipmentSlotProvider();

		if (equipmentSlotProvider != null) {
			info.setReturnValue(equipmentSlotProvider.getPreferredEquipmentSlot(stack));
		}
	}
}
