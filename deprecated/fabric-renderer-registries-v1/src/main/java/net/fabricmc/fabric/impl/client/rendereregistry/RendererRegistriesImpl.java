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

package net.fabricmc.fabric.impl.client.rendereregistry;

import net.minecraftforge.fml.common.Mod;

/**
 * Forge entrypoint placeholder.
 *
 * <p>This module ships a fabric.mod.json (so forge mod metadata is generated for it), but the
 * module itself has no Forge glue code of its own. Forge requires every declared mod id to have a
 * matching @Mod entrypoint class, otherwise mod loading aborts with
 * "constructed 0 mods: [], but had 1 mods specified".
 */
@Mod(RendererRegistriesImpl.MOD_ID)
public class RendererRegistriesImpl {
	public static final String MOD_ID = "fabric_renderer_registries_v1";

	public RendererRegistriesImpl() {
	}
}
