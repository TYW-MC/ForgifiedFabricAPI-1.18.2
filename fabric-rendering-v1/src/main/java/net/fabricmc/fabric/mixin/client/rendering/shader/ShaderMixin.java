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

package net.fabricmc.fabric.mixin.client.rendering.shader;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import net.minecraft.client.gl.Program;
import net.minecraft.client.render.Shader;
import net.minecraft.resource.ResourceFactory;
import net.minecraft.util.Identifier;

import net.fabricmc.fabric.impl.client.rendering.FabricShader;

@Mixin(Shader.class)
abstract class ShaderMixin {
	// 1.18.2 port note:
	// 1.20.1 版本里这里还有第二个注入点，用来把 `new Identifier(id)` 的入参重写成
	// FabricShader 的“命名空间前置”形式：
	//
	//   @ModifyArg(method = "<init>", at = @At(value = "INVOKE",
	//       target = "Lnet/minecraft/util/Identifier;<init>(Ljava/lang/String;)V"), allow = 1)
	//   private String modifyProgramId(String id) { ... 读 this.name ... }
	//
	// 但 1.18.2 的 `ShaderInstance(ResourceProvider, String, VertexFormat)` 是一个**委托构造器**，
	// 它在调用 `this(...)` 之前就先执行 `new ResourceLocation(name)`：
	//   0: aload_0 / 1: aload_1 / 2: new ResourceLocation / 6: aload_2
	//   7: invokespecial ResourceLocation.<init>(String)
	//  11: invokespecial <init>(ResourceProvider, ResourceLocation, VertexFormat)
	// 位置在 this() 之前，Mixin 0.8.5 因此强制要求 handler 为 static 且不可访问实例状态
	// （Injector#checkTargetForNode 抛 "@ModifyArg handler before this() invocation must be static"），
	// 而 1.20.1 的实现在该点读取 `Shader.name` 字段，无法机械照搬。
	// 另外在 1.18.2 下该重写恒为恒等变换：委托构造器的 String 参数同时充当
	// rewriteAsId 的 input 与 containedId，rewriteAsId(s, s) == s。
	// 因此这里直接删除该注入点（顺带删除没有 refmap 条目、必然会失败的
	// `@Shadow @Final String name`），只保留对 `loadProgram` 里 stage 路径的重写 —— 那个才是有实际作用的。

	// Allow loading shader stages from arbitrary namespaces.
	//
	// 1.18.2 Forge port note (crash: "Non [a-z0-9/._-] character in path of location:
	// forge:forge:shaders/core/rendertype_entity_unlit_translucent.vsh"):
	//   vanilla `ShaderInstance.loadProgram` blindly concatenates the raw shader name into the
	//   resource path, so a namespaced name (`"vertex": "mymod:foo"`) yields the unparseable
	//   string `shaders/core/mymod:foo.vsh`. That is exactly what rewriteAsId() repairs.
	//   1.18.2 Forge binpatches loadProgram to parse the name as a ResourceLocation FIRST and
	//   then build `shaders/core/<getPath()><ext>` itself, so the string we see here is already
	//   a clean path with no namespace inside it. Rewriting it unconditionally prepended the
	//   namespace a second time and blew up in ResourceLocation's path validation.
	//   -> only rewrite when the raw string really does contain the namespaced name.
	@ModifyVariable(method = "loadProgram", at = @At("STORE"), ordinal = 1)
	private static String modifyStageId(String id, ResourceFactory factory, Program.Type type, String name) {
		if (name.contains(String.valueOf(Identifier.NAMESPACE_SEPARATOR)) && id.contains(name)) {
			return FabricShader.rewriteAsId(id, name);
		}

		return id;
	}
}
