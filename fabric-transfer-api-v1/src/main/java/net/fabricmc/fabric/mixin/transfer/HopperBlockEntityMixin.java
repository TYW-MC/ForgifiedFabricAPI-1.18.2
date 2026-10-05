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

package net.fabricmc.fabric.mixin.transfer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.block.BlockState;
import net.minecraft.block.HopperBlock;
import net.minecraft.block.entity.Hopper;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;

/**
 * Allows hoppers to interact with ItemVariant storages.
 *
 * <p>1.18.2 note: forge's binpatch removes the vanilla {@code insert} method and rewrites the hopper
 * flow around {@code VanillaInventoryCodeHooks} + {@code ejectItems}, and the LVT layout of
 * {@code extract} gained a leading {@code Boolean} local. Both hooks are therefore retargeted
 * ({@code ejectItems} instead of {@code insert}) and rewritten without LocalCapture, using
 * {@link HopperBlockEntityInvoker} to re-run the vanilla lookups directly.
 */
@Mixin(HopperBlockEntity.class)
public class HopperBlockEntityMixin {
	@Inject(
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/block/entity/HopperBlockEntity;getOutputInventory(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Lnet/minecraft/inventory/Inventory;"
			),
			method = "ejectItems(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/block/entity/HopperBlockEntity;)Z",
			cancellable = true
	)
	private static void hookInsert(World world, BlockPos pos, BlockState state, HopperBlockEntity blockEntity, CallbackInfoReturnable<Boolean> cir) {
		// Let vanilla handle the transfer if it found an inventory.
		if (HopperBlockEntityInvoker.fabric_getOutputInventory(world, pos, state) != null) return;

		// Otherwise inject our transfer logic.
		Direction direction = state.get(HopperBlock.FACING);
		BlockPos targetPos = pos.offset(direction);
		Storage<ItemVariant> target = ItemStorage.SIDED.find(world, targetPos, direction.getOpposite());

		if (target != null) {
			long moved = StorageUtil.move(
					InventoryStorage.of(blockEntity, direction),
					target,
					iv -> true,
					1,
					null
			);
			cir.setReturnValue(moved == 1);
		}
	}

	@Inject(
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/block/entity/HopperBlockEntity;getInputInventory(Lnet/minecraft/world/World;Lnet/minecraft/block/entity/Hopper;)Lnet/minecraft/inventory/Inventory;"
			),
			method = "extract(Lnet/minecraft/world/World;Lnet/minecraft/block/entity/Hopper;)Z",
			cancellable = true
	)
	private static void hookExtract(World world, Hopper hopper, CallbackInfoReturnable<Boolean> cir) {
		// Let vanilla handle the transfer if it found an inventory.
		if (HopperBlockEntityInvoker.fabric_getInputInventory(world, hopper) != null) return;

		// Otherwise inject our transfer logic.
		BlockPos sourcePos = new BlockPos(hopper.getHopperX(), hopper.getHopperY() + 1.0D, hopper.getHopperZ());
		Storage<ItemVariant> source = ItemStorage.SIDED.find(world, sourcePos, Direction.DOWN);

		if (source != null) {
			long moved = StorageUtil.move(
					source,
					InventoryStorage.of(hopper, Direction.UP),
					iv -> true,
					1,
					null
			);
			cir.setReturnValue(moved == 1);
		}
	}
}
