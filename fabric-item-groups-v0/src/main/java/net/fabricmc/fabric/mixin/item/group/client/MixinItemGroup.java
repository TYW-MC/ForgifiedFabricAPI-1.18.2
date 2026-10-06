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

package net.fabricmc.fabric.mixin.item.group.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.item.ItemGroup;

/**
 * [Forge port] 原版 1.18.2 Fabric 的此 mixin 会用 {@code (index-12)%9} 的公式
 * 重写 {@code isTopRow}/{@code getColumn}，但 Forge 1.18.2 原生已为创造栏标签
 * 提供分页（每页 10 个，{@code ((index-12)%10)%5}）。若再用旧公式覆盖会导致第
 * 11 个及之后的模组标签回卷到第一页首位、与其他模组标签重叠。
 *
 * <p>因此这里不再注入任何逻辑，标签定位完全交给 Forge 原生实现。
 * 详见 2026-10-06 ocelotsignmod 与 mishanguc 标签重叠的排查。</p>
 */
@Mixin(ItemGroup.class)
public abstract class MixinItemGroup {
	@Shadow
	public abstract int getIndex();

	@Shadow
	public abstract boolean isTopRow();
}
