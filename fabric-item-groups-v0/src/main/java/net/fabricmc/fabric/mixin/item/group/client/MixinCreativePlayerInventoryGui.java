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
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.gui.screen.ingame.AbstractInventoryScreen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemGroup;
import net.minecraft.text.Text;

import net.fabricmc.fabric.impl.item.group.CreativeGuiExtensions;
import net.fabricmc.fabric.impl.item.group.FabricCreativeGuiComponents;

@Mixin(CreativeInventoryScreen.class)
public abstract class MixinCreativePlayerInventoryGui extends AbstractInventoryScreen implements CreativeGuiExtensions {
	public MixinCreativePlayerInventoryGui(ScreenHandler container_1, PlayerInventory playerInventory_1, Text textComponent_1) {
		super(container_1, playerInventory_1, textComponent_1);
	}

	@Shadow
	protected abstract void setSelectedTab(ItemGroup itemGroup_1);

	@Shadow
	public abstract int getSelectedTab(); /* XXX getSelectedTab XXX */

	// "static" matches selectedTab
	private static int fabric_currentPage = 0;

	private int fabric_getPageOffset(int page) {
		switch (page) {
		case 0:
			return 0;
		case 1:
			return 12;
		default:
			return 12 + ((12 - FabricCreativeGuiComponents.COMMON_GROUPS.size()) * (page - 1));
		}
	}

	private int fabric_getOffsetPage(int offset) {
		if (offset < 12) {
			return 0;
		} else {
			return 1 + ((offset - 12) / (12 - FabricCreativeGuiComponents.COMMON_GROUPS.size()));
		}
	}

	@Override
	public void fabric_nextPage() {
		if (fabric_getPageOffset(fabric_currentPage + 1) >= ItemGroup.GROUPS.length) {
			return;
		}

		fabric_currentPage++;
		fabric_updateSelection();
	}

	@Override
	public void fabric_previousPage() {
		if (fabric_currentPage == 0) {
			return;
		}

		fabric_currentPage--;
		fabric_updateSelection();
	}

	@Override
	public boolean fabric_isButtonVisible(FabricCreativeGuiComponents.Type type) {
		return ItemGroup.GROUPS.length > 12;
	}

	@Override
	public boolean fabric_isButtonEnabled(FabricCreativeGuiComponents.Type type) {
		if (type == FabricCreativeGuiComponents.Type.NEXT) {
			return !(fabric_getPageOffset(fabric_currentPage + 1) >= ItemGroup.GROUPS.length);
		}

		if (type == FabricCreativeGuiComponents.Type.PREVIOUS) {
			return fabric_currentPage != 0;
		}

		return false;
	}

	// [Forge port] Forge 1.18.2 的创造界面自带原生分页（tabPage/maxPages，大箭头 + 页码），
	// 由 CreativeModeInventoryScreen 按 GROUPS 下标分页渲染。fabric 自己那套分页
	// （小箭头 + 按页隐藏标签）与其冲突——表现为 Forge 翻到第 2 页后标签仍被 fabric 隐藏，
	// 必须再把 fabric 页码翻过去才能看到。这里直接全部交由 Forge 原生分页接管：
	// 1. 不再隐藏任何标签；2. 不添加 fabric 小翻页按钮；3. 不再强制切换选中标签。

	private void fabric_updateSelection() {
		// [Forge port] no-op: 交由 Forge 原生分页管理
	}

	@Inject(method = "init", at = @At("RETURN"))
	private void init(CallbackInfo info) {
		// [Forge port] no-op: 不添加 fabric 翻页按钮，由 Forge 原生翻页按钮接管
	}

	@Inject(method = "setSelectedTab", at = @At("HEAD"), cancellable = true)
	private void setSelectedTab(ItemGroup itemGroup, CallbackInfo info) {
		if (!fabric_isGroupVisible(itemGroup)) {
			info.cancel();
		}
	}

	@Inject(method = "renderTabTooltipIfHovered", at = @At("HEAD"), cancellable = true)
	private void renderTabTooltipIfHovered(MatrixStack matrixStack, ItemGroup itemGroup, int mx, int my, CallbackInfoReturnable<Boolean> info) {
		if (!fabric_isGroupVisible(itemGroup)) {
			info.setReturnValue(false);
		}
	}

	@Inject(method = "isClickInTab", at = @At("HEAD"), cancellable = true)
	private void isClickInTab(ItemGroup itemGroup, double mx, double my, CallbackInfoReturnable<Boolean> info) {
		if (!fabric_isGroupVisible(itemGroup)) {
			info.setReturnValue(false);
		}
	}

	@Inject(method = "renderTabIcon", at = @At("HEAD"), cancellable = true)
	private void renderTabIcon(MatrixStack matrixStack, ItemGroup itemGroup, CallbackInfo info) {
		if (!fabric_isGroupVisible(itemGroup)) {
			info.cancel();
		}
	}

	private boolean fabric_isGroupVisible(ItemGroup itemGroup) {
		// [Forge port] 全部可见，交给 Forge 原生 tabPage 分页渲染
		return true;
	}

	@Override
	public int fabric_currentPage() {
		return fabric_currentPage;
	}
}
