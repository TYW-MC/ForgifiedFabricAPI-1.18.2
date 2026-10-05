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

package net.fabricmc.fabric.mixin.event.lifecycle.client;

import java.util.Map;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientBlockEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents;

@Environment(EnvType.CLIENT)
@Mixin(WorldChunk.class)
abstract class WorldChunkMixin {
	@Shadow
	public abstract World getWorld();

	/*
	 * @Inject(method = "setBlockEntity", at = @At(value = "CONSTANT", args = "nullValue=true"), locals = LocalCapture.CAPTURE_FAILEXCEPTION)
	 *
	 * i509VCB: Yes this is very brittle.
	 * Sadly mixin does not want to cooperate with the Inject annotation commented out above.
	 * Our goal is to place the inject JUST after the possibly removed block entity is stored onto the stack so we can use local capture:
	 *
	 *  INVOKEVIRTUAL net/minecraft/util/math/BlockPos.toImmutable ()Lnet/minecraft/util/math/BlockPos;
	 *  ALOAD 1
	 *  INVOKEINTERFACE java/util/Map.put (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object; (itf)
	 *  CHECKCAST net/minecraft/block/entity/BlockEntity
	 *  ASTORE 3
	 *  <======== HERE
	 * L6
	 */
	@Inject(method = "setBlockEntity", at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", shift = At.Shift.BY, by = 3), locals = LocalCapture.CAPTURE_FAILEXCEPTION)
	private void onLoadBlockEntity(BlockEntity blockEntity, CallbackInfo ci, BlockPos blockPos, @Nullable BlockEntity removedBlockEntity) {
		// Only fire the load event if the block entity has actually changed
		if (blockEntity != null && blockEntity != removedBlockEntity) {
			if (this.getWorld() instanceof ServerWorld) {
				ServerBlockEntityEvents.BLOCK_ENTITY_LOAD.invoker().onLoad(blockEntity, (ServerWorld) this.getWorld());
			} else if (this.getWorld() instanceof ClientWorld) {
				ClientBlockEntityEvents.BLOCK_ENTITY_LOAD.invoker().onLoad(blockEntity, (ClientWorld) this.getWorld());
			}
		}
	}

	@Inject(method = "setBlockEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/entity/BlockEntity;markRemoved()V", shift = At.Shift.AFTER), locals = LocalCapture.CAPTURE_FAILEXCEPTION)
	private void onRemoveBlockEntity(BlockEntity blockEntity, CallbackInfo info, BlockPos blockPos, @Nullable BlockEntity removedBlockEntity) {
		if (removedBlockEntity != null) {
			if (this.getWorld() instanceof ServerWorld) {
				ServerBlockEntityEvents.BLOCK_ENTITY_UNLOAD.invoker().onUnload(removedBlockEntity, (ServerWorld) this.getWorld());
			} else if (this.getWorld() instanceof ClientWorld) {
				ClientBlockEntityEvents.BLOCK_ENTITY_UNLOAD.invoker().onUnload(removedBlockEntity, (ClientWorld) this.getWorld());
			}
		}
	}

	// 1.18.2 note: the 1.20.1 implementation redirected a Map.remove call that sat *after*
	// WorldChunk#createBlockEntity inside getBlockEntity(BlockPos, CreationType). In the forge
	// patched 1.18.2 body the two Map.remove calls both happen before createBlockEntity (an
	// isRemoved() cleanup of the block entity map and a blockEntityNbts lookup), so the slice no
	// longer matches and the redirect fails its injection check. Retargeting it to the earlier
	// removal would fire BLOCK_ENTITY_UNLOAD for a block entity that was already removed - and
	// would double-fire together with the injectors below - so the redirect was dropped.
	// Both regular unload paths are still covered by the setBlockEntity/removeBlockEntity injectors.
	// (This mirrors the identical fix already applied to server/WorldChunkMixin.java.)

	@Inject(method = "removeBlockEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/entity/BlockEntity;markRemoved()V"), locals = LocalCapture.CAPTURE_FAILEXCEPTION)
	private void onRemoveBlockEntity(BlockPos pos, CallbackInfo ci, @Nullable BlockEntity removed) {
		if (removed != null) {
			if (this.getWorld() instanceof ServerWorld) {
				ServerBlockEntityEvents.BLOCK_ENTITY_UNLOAD.invoker().onUnload(removed, (ServerWorld) this.getWorld());
			} else if (this.getWorld() instanceof ClientWorld) {
				ClientBlockEntityEvents.BLOCK_ENTITY_UNLOAD.invoker().onUnload(removed, (ClientWorld) this.getWorld());
			}
		}
	}
}
