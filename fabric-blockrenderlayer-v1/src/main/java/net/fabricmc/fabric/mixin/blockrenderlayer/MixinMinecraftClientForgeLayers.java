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

package net.fabricmc.fabric.mixin.blockrenderlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.MinecraftClient;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.impl.blockrenderlayer.BlockRenderLayerMapImpl;

/**
 * Applies the Forge-side render layers that {@code BlockRenderLayerMapImpl#registerForgeLayers}
 * had to park.
 *
 * <p>On Forge, block render layers live in two maps and Forge's {@code setRenderLayer} is the only
 * way to write the second one. That method cannot be called while the client instance is missing
 * though: Embeddium/Rubidium mix into it and their callback uses {@code Minecraft.getInstance()},
 * which throws a NPE during the early Fabric entrypoint stage (the connector invokes those from
 * {@code Main#main}, before the client object exists) - and a NPE thrown there fails the whole
 * class initialiser and aborts the game's startup.
 *
 * <p>The first client tick is both late enough for the client instance to exist and early enough
 * that nothing has been rendered yet, so the parked layers are applied from here.
 */
@Environment(EnvType.CLIENT)
@Mixin(MinecraftClient.class)
public abstract class MixinMinecraftClientForgeLayers {
	@Inject(at = @At("HEAD"), method = "tick")
	private void flushPendingForgeLayers(CallbackInfo info) {
		BlockRenderLayerMapImpl.flushForgeLayers();
	}
}
