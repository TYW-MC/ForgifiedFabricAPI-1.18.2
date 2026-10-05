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

package net.fabricmc.fabric.impl.biome;

import net.minecraftforge.fml.common.Mod;

/**
 * Forge entrypoint of this module.
 *
 * <p>1.18.2 note: Forge's {@code BIOME_MODIFIER_SERIALIZERS} registry (and the whole
 * {@code BiomeModifier} system) only exists from 1.19 onwards, so there is nothing to register
 * here on 1.18.2. Biome modification itself is implemented entirely through this module's mixins
 * ({@code MixinBiomeSource}, {@code MixinTheEndBiomeSource}, ...).
 */
@Mod(BiomeApiImpl.MOD_ID)
public class BiomeApiImpl {
	public static final String MOD_ID = "fabric_biome_api_v1";

	public BiomeApiImpl() {
	}
}
