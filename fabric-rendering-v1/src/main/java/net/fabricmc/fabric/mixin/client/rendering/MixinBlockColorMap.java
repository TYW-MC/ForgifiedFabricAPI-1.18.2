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

package net.fabricmc.fabric.mixin.client.rendering;

import java.util.Map;

import net.minecraftforge.registries.IRegistryDelegate;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.block.Block;
import net.minecraft.client.color.block.BlockColorProvider;
import net.minecraft.client.color.block.BlockColors;

import net.fabricmc.fabric.impl.client.rendering.ColorProviderRegistryImpl;

@Mixin(BlockColors.class)
public class MixinBlockColorMap implements ColorProviderRegistryImpl.ColorMapperHolder<Block, BlockColorProvider> {
	// 1.18.2 port note: 与 MixinItemColorMap 同理 —— Forge 把 `BlockColors.providers`
	// 从 `IdList<BlockColor>` 换成了 `Map<IRegistryDelegate<Block>, BlockColor>`，
	// refmap 无法生成条目，这里直接使用运行时 SRG 名 f_92571_。
	@Shadow
	@Final
	private Map<IRegistryDelegate<Block>, BlockColorProvider> f_92571_;

	@Inject(method = "create", at = @At("RETURN"))
	private static void create(CallbackInfoReturnable<BlockColors> info) {
		ColorProviderRegistryImpl.BLOCK.initialize(info.getReturnValue());
	}

	@Override
	public BlockColorProvider get(Block block) {
		// 1.18.2 Forge 的 map key 是方块的 Forge 注册代理。
		// Block 本身不直接继承 ForgeRegistryEntry，但它的父类 BlockBehaviour 继承了
		// ForgeRegistryEntry<Block>，所以 block.delegate 可访问。
		return f_92571_.get(block.delegate);
	}
}
