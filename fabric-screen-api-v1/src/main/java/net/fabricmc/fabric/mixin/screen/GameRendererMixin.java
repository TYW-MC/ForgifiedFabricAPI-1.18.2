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

package net.fabricmc.fabric.mixin.screen;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Matrix4f;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;

@Environment(EnvType.CLIENT)
@Mixin(GameRenderer.class)
abstract class GameRendererMixin {
	@Shadow
	@Final
	private MinecraftClient client;

	@Unique
	private Screen renderingScreen;

	// NOTE(1): Forge binpatches GameRenderer.render so the vanilla
	//   invokevirtual Screen.render(PoseStack;IIF)V
	// is replaced in-place by
	//   invokestatic net/minecraftforge/client/ForgeHooksClient.drawScreen(Screen;PoseStack;IIF)V
	// (verified with javap on forge-1.18.2-40.3.12-client.jar vs client-...-srg.jar). ForgeHooksClient.drawScreen()
	// is a thin wrapper that does poseStack.pushPose() -> fire GUI layer events -> Screen.render(...) ->
	// poseStack.popPose(), so injecting around that call keeps the original "just before/after the screen renders"
	// semantics.
	// The target literal must stay inline (a constant reference would stop the refmap generator
	// from emitting the remapped descriptor).
	//
	// NOTE(2) LocalCapture in 1.18.2: Mixin matches captured locals *strictly positionally*, starting at
	//   slot frameSize (i.e. the first local after this+args), and it does NOT skip intermediate locals it
	//   cannot map. In 1.18.2 GameRenderer.render() the LocalVariableTable at the drawScreen call site is:
	//      5: int        i          <- mouseX passed to drawScreen (iload 5)
	//      6: int        j          <- mouseY passed to drawScreen (iload 6)
	//      7: Window     window     (unused)
	//      8: Matrix4f   matrix4f   (unused)
	//      9: PoseStack  poseStack  (unused)
	//     10: PoseStack  poseStack1 <- the PoseStack passed to drawScreen (aload 10)
	//   The upstream (1.20.1) signature `(int, int, MatrixStack)` happened to line up there, but on 1.18.2
	//   the 3rd local is `Window`, producing:
	//     "LVT ... has incompatible changes ... Expected: [I, I, PoseStack] Found: [I, I, Window]"
	//   We therefore declare every local from slot 5 up to and including the one we actually need (slot 10)
	//   in exact slot order, which is what Mixin requires.

	@Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/client/ForgeHooksClient;drawScreen(Lnet/minecraft/client/gui/screen/Screen;Lnet/minecraft/client/util/math/MatrixStack;IIF)V"), locals = LocalCapture.CAPTURE_FAILEXCEPTION)
	private void onBeforeRenderScreen(float tickDelta, long startTime, boolean tick, CallbackInfo ci, int mouseX, int mouseY, Window localWindow, Matrix4f localMatrix4f, MatrixStack localPoseStack, MatrixStack matrices) {
		// Store the screen in a variable in case someone tries to change the screen during this before render event.
		// If someone changes the screen, the after render event will likely have class cast exceptions or an NPE.
		this.renderingScreen = this.client.currentScreen;
		ScreenEvents.beforeRender(this.renderingScreen).invoker().beforeRender(this.renderingScreen, matrices, mouseX, mouseY, tickDelta);
	}

	// This injection should end up in the try block so exceptions are caught
	@Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/client/ForgeHooksClient;drawScreen(Lnet/minecraft/client/gui/screen/Screen;Lnet/minecraft/client/util/math/MatrixStack;IIF)V", shift = At.Shift.AFTER), locals = LocalCapture.CAPTURE_FAILEXCEPTION)
	private void onAfterRenderScreen(float tickDelta, long startTime, boolean tick, CallbackInfo ci, int mouseX, int mouseY, Window localWindow, Matrix4f localMatrix4f, MatrixStack localPoseStack, MatrixStack matrices) {
		ScreenEvents.afterRender(this.renderingScreen).invoker().afterRender(this.renderingScreen, matrices, mouseX, mouseY, tickDelta);
		// Finally set the currently rendering screen to null
		this.renderingScreen = null;
	}
}
