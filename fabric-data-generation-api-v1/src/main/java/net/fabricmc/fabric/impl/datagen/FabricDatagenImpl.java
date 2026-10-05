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

package net.fabricmc.fabric.impl.datagen;

import net.minecraftforge.fml.common.Mod;

/**
 * Forge entrypoint of this module.
 *
 * <p>1.18.2 port note: the Sinytra Forge glue layer of this module (Forge event adapters such
 * as the {@code *EventHooks} / {@code *ClientInit} helpers) was not part of this port. The
 * module's behaviour is implemented directly by its own mixins instead, so this class only
 * exists so that Forge can construct the mod container (a mods.toml declaring mod id
 * "fabric_data_generation_api_v1" without a matching {@code @Mod} entrypoint is a hard loading error for Forge).
 */
@Mod(FabricDatagenImpl.MOD_ID)
public class FabricDatagenImpl {
	public static final String MOD_ID = "fabric_data_generation_api_v1";

	public FabricDatagenImpl() {
	}
}
