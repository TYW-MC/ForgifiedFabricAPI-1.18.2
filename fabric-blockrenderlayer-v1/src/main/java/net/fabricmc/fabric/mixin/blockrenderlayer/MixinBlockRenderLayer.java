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

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.block.Block;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.fluid.Fluid;

import net.fabricmc.fabric.impl.blockrenderlayer.BlockRenderLayerMapImpl;

@Mixin(RenderLayers.class)
public class MixinBlockRenderLayer {
	@Shadow private static Map<Block, RenderLayer> BLOCKS;
	@Shadow private static Map<Fluid, RenderLayer> FLUIDS;

	@Inject(method = "<clinit>*", at = @At("RETURN"))
	private static void onInitialize(CallbackInfo info) {
		BlockRenderLayerMapImpl.initialize(BLOCKS::put, FLUIDS::put);

		// Forge note: writing BLOCKS/FLUIDS alone is not enough on Forge. RenderLayers derives
		// its predicate map (blockRenderChecks, read by canRenderInLayer) from BLOCKS during
		// this very initialiser and never looks at BLOCKS again, so registrations that only
		// reach BLOCKS are invisible to anything asking Forge - in practice the optimisation
		// mods Embeddium/Rubidium, which rendered those blocks as solid (transparent texture
		// pixels drawn opaque). registerForgeLayers hands every layer to Forge's
		// RenderLayers.setRenderLayer, the API that maintains that second map, but parks it
		// until the client instance exists: setRenderLayer is hooked by Embeddium, whose
		// callback dereferences Minecraft.getInstance(), and this initialiser can run before
		// the client exists.
		BlockRenderLayerMapImpl.registerForgeLayers();
	}
}
