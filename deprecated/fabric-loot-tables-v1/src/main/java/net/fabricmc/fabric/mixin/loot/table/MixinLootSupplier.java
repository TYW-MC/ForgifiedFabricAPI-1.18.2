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

package net.fabricmc.fabric.mixin.loot.table;

import java.util.List;

import com.google.common.collect.ImmutableList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.function.LootFunction;

import net.fabricmc.fabric.api.loot.v1.FabricLootSupplier;

/**
 * 1.18.2 port note: currently NOT registered in {@code fabric-loot-tables-v1.mixins.json}.
 *
 * <p>This module's mixin refmap is generated empty, so the {@code @Shadow} fields below can never be
 * resolved at runtime (mixin validates shadow fields by name <em>and</em> descriptor, and the runtime
 * descriptor uses the mojang package names). Registering this mixin therefore aborts mod loading with
 * "@Shadow field pools was not located in the target class ...LootTable".
 *
 * <p>Keeping the type here for reference; the working replacement is a two-part split (a plain
 * {@code @Accessor} interface mixin plus a class mixin that delegates to it), which is still to be done.
 */
@Mixin(LootTable.class)
public abstract class MixinLootSupplier implements FabricLootSupplier {
	@Shadow
	@Final
	LootPool[] pools;

	@Shadow
	@Final
	LootFunction[] functions;

	@Override
	public List<LootPool> getPools() {
		return ImmutableList.copyOf(pools);
	}

	@Override
	public List<LootFunction> getFunctions() {
		return ImmutableList.copyOf(functions);
	}
}
