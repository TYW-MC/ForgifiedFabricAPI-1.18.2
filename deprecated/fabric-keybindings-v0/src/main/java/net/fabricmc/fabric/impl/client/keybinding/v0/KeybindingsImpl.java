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

package net.fabricmc.fabric.impl.client.keybinding.v0;

import net.minecraftforge.fml.common.Mod;

/**
 * Forge entrypoint placeholder (1.18.2 port).
 *
 * <p>Kept in a versioned sub-package on purpose: the modern module fabric-key-binding-api-v1
 * already exports net.fabricmc.fabric.impl.client.keybinding, and two automatic modules exporting
 * the same package aborts the JPMS module layer with a ResolutionException.
 */
@Mod(KeybindingsImpl.MOD_ID)
public class KeybindingsImpl {
	public static final String MOD_ID = "fabric_keybindings_v0";

	public KeybindingsImpl() {
	}
}
