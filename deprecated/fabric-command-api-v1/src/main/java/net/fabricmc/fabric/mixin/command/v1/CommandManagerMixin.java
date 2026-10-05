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

package net.fabricmc.fabric.mixin.command.v1;

import com.mojang.brigadier.CommandDispatcher;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;

@Mixin(CommandManager.class)
public abstract class CommandManagerMixin {
	@Shadow
	@Final
	private CommandDispatcher<ServerCommandSource> dispatcher;

	/**
	 * 1.18.2 port note: upstream targets the {@code CommandDispatcher#findAmbiguities} call inside
	 * the constructor, which relies on Fabric's mixin fork (vanilla Mixin 0.8.5 rejects an
	 * {@code @At(INVOKE)} injection point inside a constructor with
	 * "Found @Inject targetting a constructor"). Injecting at the constructor's RETURN is the
	 * supported alternative: mod commands are registered slightly later (after ambiguity analysis)
	 * but remain fully functional.
	 */
	@Inject(at = @At("RETURN"), method = "<init>")
	private void fabric_addCommands(CommandManager.RegistrationEnvironment environment, CallbackInfo ci) {
		CommandRegistrationCallback.EVENT.invoker().register(this.dispatcher, environment == CommandManager.RegistrationEnvironment.DEDICATED);
	}
}
