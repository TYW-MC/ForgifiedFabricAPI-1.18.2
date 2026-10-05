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

package net.fabricmc.fabric.mixin.datagen.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.MinecraftClient;

import net.fabricmc.fabric.impl.datagen.FabricDataGenHelper;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
	// 1.18.2 port note:
	// 1.20.1 的注入点是构造器里的一个方法调用：
	//   @At(value = "INVOKE",
	//       target = "Lcom/mojang/blaze3d/systems/RenderSystem;getBackendDescription()Ljava/lang/String;")
	// 这在 1.18.2 上会直接被 Mixin 拒绝，理由与实现无关：
	// `@At("INVOKE")` 的 RestrictTargetLevel 是 METHODS_ONLY，而目标方法是构造器
	// （org.spongepowered.asm.mixin.injection.code.Injector#checkTargetForNode 的
	//  `if (target.isCtor && targetLevel == METHODS_ONLY) throw "Found %s targetting a constructor..."`）。
	// 另外 1.18.2 的 Minecraft 构造器里根本没有 getBackendDescription() 调用，
	// 所以也无法靠换一个 target 解决。
	// 构造器内可用的注入点只有 HEAD / RETURN：@At("HEAD") 位于 super() 之前，
	// 会让 data-gen 在注册表/资源管理器就绪前执行；@At("RETURN") 落在构造器末尾、
	// 仍在 run() 触发资源重载之前，对 datagen 是安全的。
	// 该分支只有在 FabricDataGenHelper.ENABLED 为真（dev 环境跑 datagen）时才生效，
	// 正常游戏里是空操作。
	@Inject(method = "<init>", at = @At("RETURN"))
	private void main(CallbackInfo info) {
		if (FabricDataGenHelper.ENABLED) {
			FabricDataGenHelper.run();

			// Exit gracefully.
			System.exit(0);
		}
	}
}
