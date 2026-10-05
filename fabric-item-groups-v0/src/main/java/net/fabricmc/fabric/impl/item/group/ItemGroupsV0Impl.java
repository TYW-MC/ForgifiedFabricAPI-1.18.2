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

package net.fabricmc.fabric.impl.item.group;

import net.minecraftforge.fml.common.Mod;

/**
 * Forge entrypoint of this module.
 *
 * <p>1.18.2 port note: Forge builds one {@code ModContainer} per class annotated with
 * {@code @Mod}. {@code ModLoader#buildMods} compares the mods declared by the module's
 * {@code META-INF/mods.toml} against the containers it could actually construct from the
 * jar's scan targets; if they do not match it logs
 * {@code File ... constructed 0 mods, but had 1 mods specified: [fabric_item_groups_v0]}
 * and marks the whole game as a "broken mod state" (every Forge event is then refused).
 * Every other module in this port carries such a stub class - see
 * {@code net.fabricmc.fabric.impl.item.FabricItemImpl} - this module was missing it.
 */
@Mod(ItemGroupsV0Impl.MOD_ID)
public class ItemGroupsV0Impl {
	public static final String MOD_ID = "fabric_item_groups_v0";

	public ItemGroupsV0Impl() {
	}
}
