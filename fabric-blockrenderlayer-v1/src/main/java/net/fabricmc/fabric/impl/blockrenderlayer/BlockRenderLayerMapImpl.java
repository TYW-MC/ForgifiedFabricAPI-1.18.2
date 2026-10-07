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

package net.fabricmc.fabric.impl.blockrenderlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.Item;

import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;

public class BlockRenderLayerMapImpl implements BlockRenderLayerMap {
	public BlockRenderLayerMapImpl() { }

	@Override
	public void putBlock(Block block, RenderLayer renderLayer) {
		if (block == null) throw new IllegalArgumentException("Request to map null block to BlockRenderLayer");
		if (renderLayer == null) throw new IllegalArgumentException("Request to map block " + block.toString() + " to null BlockRenderLayer");

		blockHandler.accept(block, renderLayer);
	}

	@Override
	public void putBlocks(RenderLayer renderLayer, Block... blocks) {
		for (Block block : blocks) {
			putBlock(block, renderLayer);
		}
	}

	@Override
	public void putItem(Item item, RenderLayer renderLayer) {
		if (item == null) throw new IllegalArgumentException("Request to map null item to BlockRenderLayer");
		if (renderLayer == null) throw new IllegalArgumentException("Request to map item " + item.toString() + " to null BlockRenderLayer");

		itemHandler.accept(item, renderLayer);
	}

	@Override
	public void putItems(RenderLayer renderLayer, Item... items) {
		for (Item item : items) {
			putItem(item, renderLayer);
		}
	}

	@Override
	public void putFluid(Fluid fluid, RenderLayer renderLayer) {
		if (fluid == null) throw new IllegalArgumentException("Request to map null fluid to BlockRenderLayer");
		if (renderLayer == null) throw new IllegalArgumentException("Request to map fluid " + fluid.toString() + " to null BlockRenderLayer");

		fluidHandler.accept(fluid, renderLayer);
	}

	@Override
	public void putFluids(RenderLayer renderLayer, Fluid... fluids) {
		for (Fluid fluid : fluids) {
			putFluid(fluid, renderLayer);
		}
	}

	private static Map<Block, RenderLayer> blockRenderLayerMap = new HashMap<>();
	private static Map<Item, RenderLayer> itemRenderLayerMap = new HashMap<>();
	private static Map<Fluid, RenderLayer> fluidRenderLayerMap = new HashMap<>();

	//Forge-side layers waiting for the client instance to exist. Guarded by pendingForgeBlockLayers.
	private static final Map<Block, RenderLayer> pendingForgeBlockLayers = new HashMap<>();
	private static final Map<Fluid, RenderLayer> pendingForgeFluidLayers = new HashMap<>();
	private static boolean forgeLayersApplied = false;

	//This consumers initially add to the maps above, and then are later set (when initialize is called) to insert straight into the target map.
	private static BiConsumer<Block, RenderLayer> blockHandler = (b, l) -> blockRenderLayerMap.put(b, l);
	private static BiConsumer<Item, RenderLayer> itemHandler = (i, l) -> itemRenderLayerMap.put(i, l);
	private static BiConsumer<Fluid, RenderLayer> fluidHandler = (f, b) -> fluidRenderLayerMap.put(f, b);

	public static void initialize(BiConsumer<Block, RenderLayer> blockHandlerIn, BiConsumer<Fluid, RenderLayer> fluidHandlerIn) {
		//Done to handle backwards compat, in previous snapshots Items had their own map for render layers, now the BlockItem is used.
		BiConsumer<Item, RenderLayer> itemHandlerIn = (item, renderLayer) -> blockHandlerIn.accept(Block.getBlockFromItem(item), renderLayer);

		//Add all the pre existing render layers
		blockRenderLayerMap.forEach(blockHandlerIn);
		itemRenderLayerMap.forEach(itemHandlerIn);
		fluidRenderLayerMap.forEach(fluidHandlerIn);

		//Set the handlers to directly accept later additions
		blockHandler = blockHandlerIn;
		itemHandler = itemHandlerIn;
		fluidHandler = fluidHandlerIn;
	}

	/**
	 * Forge half of the registration, called by the {@code RenderLayers} mixin right after
	 * {@link #initialize}.
	 *
	 * <p><b>Why this is needed (1.18.2 port).</b> On Forge, {@code RenderLayers} (vanilla
	 * {@code ItemBlockRenderTypes}) does not answer render layer questions from
	 * {@code BLOCKS}/{@code FLUIDS} alone. During its static initialiser it derives a second,
	 * predicate-shaped map from {@code BLOCKS} ({@code blockRenderChecks}, exposed through
	 * {@code getBlockLayerPredicatesView()} and consumed by {@code canRenderInLayer}). That
	 * derivation happens exactly once, so anything written into {@code BLOCKS} afterwards is
	 * invisible to it: the map keeps its default predicate, which only accepts the solid layer.
	 *
	 * <p>Vanilla's own chunk builder goes through {@code BLOCKS} and is therefore fine, but the
	 * optimisation mods Embeddium/Rubidium ask Forge instead - {@code EmbeddiumRenderLayerCache}
	 * builds a state's render type list out of {@code ItemBlockRenderTypes.canRenderInLayer} - so
	 * a Fabric block registered as cutout/translucent through this API kept being rendered with
	 * the solid layer there. The alpha channel of its texture was ignored and the transparent
	 * pixels were drawn with their own colour, which is how road line and road sign textures
	 * (transparent pixels are pure black for the yellow lines and near-white for the white ones)
	 * ended up as solid black or white faces in game.
	 *
	 * <p>{@code RenderLayers.setRenderLayer} is Forge's public API for that second map and the
	 * only thing that republishes its read-only view, so every layer is forwarded to it as well.
	 * It is registered <em>in addition to</em> the vanilla maps, never instead of them.
	 *
	 * <p><b>Why the forwarding is parked instead of done here (1.18.2 port).</b> This method runs
	 * from {@code RenderLayers}' static initialiser, and under Sinytra Connector a Fabric mod can
	 * trigger that initialiser from its client entrypoint, which {@code Main#main} invokes
	 * <em>before</em> the Minecraft client object exists. Forge's own {@code setRenderLayer}
	 * would not care, but Embeddium (and Rubidium) mix into it and its callback runs
	 * {@code Minecraft.getInstance().execute(...)} - with a null instance that is a
	 * {@link NullPointerException}. Because the call happens inside a class initialiser, the NPE
	 * becomes an {@link ExceptionInInitializerError}, the connector reports
	 * "Could not execute entrypoint stage 'client'" and the game dies while starting up. So every
	 * entry is parked in {@link #pendingForgeBlockLayers}/{@link #pendingForgeFluidLayers} here
	 * and applied by {@link #flushForgeLayers()} on the client's first tick, which is both after
	 * the client instance exists and long before anything is rendered.
	 */
	public static void registerForgeLayers() {
		//Replay everything that was registered before this point.
		blockRenderLayerMap.forEach((block, renderLayer) -> queueForgeLayer(block, renderLayer));
		itemRenderLayerMap.forEach((item, renderLayer) -> queueForgeLayer(Block.getBlockFromItem(item), renderLayer));
		fluidRenderLayerMap.forEach((fluid, renderLayer) -> queueForgeLayer(fluid, renderLayer));

		//And keep collecting later registrations, on top of the vanilla maps.
		BiConsumer<Block, RenderLayer> vanillaBlockHandler = blockHandler;
		BiConsumer<Item, RenderLayer> vanillaItemHandler = itemHandler;
		BiConsumer<Fluid, RenderLayer> vanillaFluidHandler = fluidHandler;

		blockHandler = (block, renderLayer) -> {
			vanillaBlockHandler.accept(block, renderLayer);
			queueForgeLayer(block, renderLayer);
		};
		itemHandler = (item, renderLayer) -> {
			vanillaItemHandler.accept(item, renderLayer);
			queueForgeLayer(Block.getBlockFromItem(item), renderLayer);
		};
		fluidHandler = (fluid, renderLayer) -> {
			vanillaFluidHandler.accept(fluid, renderLayer);
			queueForgeLayer(fluid, renderLayer);
		};
	}

	private static void queueForgeLayer(Block block, RenderLayer renderLayer) {
		if (forgeLayersApplied) {
			RenderLayers.setRenderLayer(block, renderLayer);
			return;
		}

		synchronized (pendingForgeBlockLayers) {
			pendingForgeBlockLayers.put(block, renderLayer);
		}
	}

	private static void queueForgeLayer(Fluid fluid, RenderLayer renderLayer) {
		if (forgeLayersApplied) {
			RenderLayers.setRenderLayer(fluid, renderLayer);
			return;
		}

		synchronized (pendingForgeBlockLayers) {
			pendingForgeFluidLayers.put(fluid, renderLayer);
		}
	}

	/**
	 * Applies the Forge-side layers parked by {@link #registerForgeLayers()} and turns the
	 * handlers into pass-through writers. Called from the client's first tick
	 * ({@code MixinMinecraftClientForgeLayers}), which is the earliest point where Forge's
	 * {@code setRenderLayer} can safely be called on a setup with Embeddium/Rubidium installed:
	 * its mixin needs {@code Minecraft.getInstance()} to be non-null.
	 *
	 * <p>Does nothing while the client instance is still missing, so the tick hook may simply call
	 * this again on the next tick.
	 */
	public static void flushForgeLayers() {
		if (forgeLayersApplied || MinecraftClient.getInstance() == null) {
			return;
		}

		Map<Block, RenderLayer> blocks;
		Map<Fluid, RenderLayer> fluids;

		synchronized (pendingForgeBlockLayers) {
			if (forgeLayersApplied) {
				return;
			}

			//Registrations that arrive from here on are written straight through.
			forgeLayersApplied = true;
			blocks = new HashMap<>(pendingForgeBlockLayers);
			fluids = new HashMap<>(pendingForgeFluidLayers);
			pendingForgeBlockLayers.clear();
			pendingForgeFluidLayers.clear();
		}

		blocks.forEach((block, renderLayer) -> RenderLayers.setRenderLayer(block, renderLayer));
		fluids.forEach((fluid, renderLayer) -> RenderLayers.setRenderLayer(fluid, renderLayer));
	}
}
