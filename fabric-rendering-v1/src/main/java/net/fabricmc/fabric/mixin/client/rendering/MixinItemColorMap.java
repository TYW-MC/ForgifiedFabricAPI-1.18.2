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

import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.color.item.ItemColorProvider;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;

import net.fabricmc.fabric.impl.client.rendering.ColorProviderRegistryImpl;

@Mixin(ItemColors.class)
public class MixinItemColorMap implements ColorProviderRegistryImpl.ColorMapperHolder<ItemConvertible, ItemColorProvider> {
	// 1.18.2 port note:
	// Forge 把 `ItemColors.providers` 的类型从 `IdList<ItemColor>` 换成了
	// `Map<IRegistryDelegate<Item>, ItemColor>`（见 forge-1.18.2-40.3.12-client.jar 里
	// ItemColors 的 javap 输出）。因为（名称, 描述符）配对不上，TinyRemapper 的 mixin 扩展
	// 无法为它生成 refmap 条目，`@Shadow` 会退化成按字面名 `providers` 去目标类里找，
	// 于是报 "@Shadow field providers was not located"。
	// 所以这里直接写运行时真实字段名 f_92674_（Forge runtime 用 SRG 名），
	// 与 fabric-particles-v1 的 ParticleManagerAccessor 使用 f_107293_ 的做法一致：
	// refmap 查不到 → 按字面名使用 → 正好命中 SRG 名。
	@Shadow
	@Final
	private Map<IRegistryDelegate<Item>, ItemColorProvider> f_92674_;

	@Inject(method = "create", at = @At("RETURN"))
	private static void create(BlockColors blockMap, CallbackInfoReturnable<ItemColors> info) {
		ColorProviderRegistryImpl.ITEM.initialize(info.getReturnValue());
	}

	@Override
	public ItemColorProvider get(ItemConvertible item) {
		// 1.18.2 Forge 的 map key 是物品的 Forge 注册代理（Item 继承
		// ForgeRegistryEntry，因此 item.asItem().delegate 可直接访问），不再是 raw id。
		return f_92674_.get(item.asItem().delegate);
	}
}
