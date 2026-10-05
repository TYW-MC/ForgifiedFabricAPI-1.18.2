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

package net.fabricmc.fabric.mixin.gamerule;

import java.lang.reflect.Field;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.world.GameRules;

import net.fabricmc.fabric.impl.gamerule.EnumRuleCommand;
import net.fabricmc.fabric.impl.gamerule.EnumRuleType;

/**
 * 1.18.2 Forge port of the upstream {@code GameRuleCommandVisitorMixin} (restored).
 *
 * <p><b>Why this mixin is mandatory:</b> vanilla {@code GameRules.visitGameRuleTypes} calls
 * {@code GameRuleCommand$1.visit(key, type)} ({@code m_6889_}) for <i>every</i> registered rule,
 * and that visitor unconditionally calls {@code type.argument(key.getId())}. {@link EnumRuleType}
 * passes {@code super(null, ...)} (null argument-type supplier, same as upstream), so without this
 * mixin every world load dies with
 * {@code NullPointerException: Cannot invoke "Supplier.get()" because "this.f_46337_" is null}
 * inside {@code GameRules$Type.m_46358_} — surfaced by the client as
 * "Errors in currently selected data packs prevent the world from running".</p>
 *
 * <p><b>Port notes (legacy mixin AP workarounds):</b></p>
 * <ul>
 *   <li>The Architectury Loom 1.3 mixin AP crashes ({@code SignaturePrinter} /
 *       {@code StringIndexOutOfBoundsException}) whenever it has to print the generic signature of
 *       {@code GameRuleCommand$1}'s {@code LiteralArgumentBuilder<...>} field — which happens for
 *       <b>any</b> {@code @Shadow}/{@code @Accessor} of that field once target matching goes
 *       fuzzy. The upstream {@code @Shadow LiteralArgumentBuilder field_19419} therefore had to be
 *       replaced with positional reflection: the anonymous class declares exactly one field
 *       (javap-verified for 1.18.2, {@code field_19419} / SRG {@code f_137760_}), so no mapping
 *       name is needed at all and reflection is immune to both the AP and runtime remapping.</li>
 *   <li>The injection target uses the name-only SRG literal {@code m_6889_} because the upstream
 *       full descriptor {@code visit(Lnet/minecraft/world/GameRules$Key;...)V} contains {@code $},
 *       which the same AP cannot process either (same workaround as {@code RuleListWidgetMixin}'s
 *       {@code m_170228_}). The AP logs a harmless "Unable to determine descriptor" warning.</li>
 *   <li>Handler parameters are deliberately raw (no {@code <T extends GameRules.Value<T>>}
 *       generics) to keep generic signatures out of the AP's hands.</li>
 * </ul>
 */
@Mixin(targets = "net/minecraft/server/command/GameRuleCommand$1")
public abstract class GameRuleCommandVisitorMixin {
	@Unique
	private static Field fabric$builderField;

	@Inject(at = @At("HEAD"), method = "m_6889_", cancellable = true)
	private void onRegisterCommand(GameRules.Key key, GameRules.Type type, CallbackInfo ci) {
		// Check if our type is a EnumRuleType
		if (type instanceof EnumRuleType) {
			//noinspection rawtypes,unchecked
			EnumRuleCommand.register(this.fabric$getBuilder(), (GameRules.Key) key, (EnumRuleType) type);
			ci.cancel();
		}
	}

	/**
	 * Reads the {@code LiteralArgumentBuilder} the vanilla visitor is assembling for
	 * {@code /gamerule} from {@code this} (the merged visitor instance) via reflection —
	 * see class javadoc for why {@code @Shadow} was not an option.
	 *
	 * <p>The target class has <b>two</b> declared fields at runtime after this mixin
	 * merges into it: the vanilla {@code LiteralArgumentBuilder} field and this mixin's
	 * own {@code @Unique} static cache field. Static fields are skipped and the
	 * remaining candidate is identified by type name.</p>
	 *
	 * <p>⚠️ Do NOT filter by {@link Field#isSynthetic()} here: the ForgeGradle SRG
	 * remapping pipeline marks anonymous-class fields {@code ACC_SYNTHETIC} in the jar
	 * itself (verified: {@code f_137760_} has {@code flags: (0x1010) ACC_FINAL,
	 * ACC_SYNTHETIC} in {@code client-1.18.2-srg.jar}), so the field we need reports
	 * {@code isSynthetic() == true} at runtime.</p>
	 */
	@Unique
	private LiteralArgumentBuilder fabric$getBuilder() {
		Field field = fabric$builderField;

		if (field == null) {
			Field typeMatch = null;
			Field singleCandidate = null;
			int candidates = 0;
			StringBuilder dump = new StringBuilder();

			for (Field f : this.getClass().getDeclaredFields()) {
				dump.append(java.lang.reflect.Modifier.isStatic(f.getModifiers()) ? "[static] " : "[cand] ")
						.append(f.getName()).append(" : ").append(f.getType().getName()).append("; ");

				// Skip only our own merged-in static cache field. Not isSynthetic() —
				// the SRG remap marks the target field itself synthetic (see javadoc).
				if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) {
					continue;
				}

				candidates++;

				if (singleCandidate == null) {
					singleCandidate = f;
				}

				if (f.getType().getName().contains("LiteralArgumentBuilder")) {
					if (typeMatch != null) {
						throw new IllegalStateException("Found multiple LiteralArgumentBuilder fields in GameRuleCommand$1");
					}

					typeMatch = f;
				}
			}

			// Prefer the type-matched builder; fall back to the single non-static candidate
			// (the vanilla anonymous class declares exactly one instance field).
			field = typeMatch != null ? typeMatch : (candidates == 1 ? singleCandidate : null);

			if (field == null) {
				throw new IllegalStateException("Could not find the /gamerule builder field in GameRuleCommand$1 (fields: " + dump + ")");
			}

			field.setAccessible(true);
			fabric$builderField = field;
		}

		try {
			return (LiteralArgumentBuilder) field.get(this);
		} catch (IllegalAccessException e) {
			throw new IllegalStateException("Could not read the /gamerule builder from GameRuleCommand$1", e);
		}
	}
}
