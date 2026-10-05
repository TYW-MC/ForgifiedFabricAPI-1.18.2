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
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.Hopper;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * 1.18.2 note: {@code getInputInventory}/{@code getOutputInventory} are private static in the
 * Forge-patched hopper, and the vanilla LVT layout around their call sites differs from 1.20.1
 * (for example a {@code Boolean} local from {@code VanillaInventoryCodeHooks.extractHook} now
 * precedes the inventory on the stack). LocalCapture therefore cannot be ported; the lookups are
 * invoked directly instead, which keeps {@link HopperBlockEntityMixin} free of LVT assumptions.
 */
@Mixin(HopperBlockEntity.class)
public interface HopperBlockEntityInvoker {
	@Invoker("getInputInventory")
	static Inventory fabric_getInputInventory(World world, Hopper hopper) {
		throw new AssertionError();
	}

	@Invoker("getOutputInventory")
	static Inventory fabric_getOutputInventory(World world, BlockPos pos, BlockState state) {
		throw new AssertionError();
	}
}
