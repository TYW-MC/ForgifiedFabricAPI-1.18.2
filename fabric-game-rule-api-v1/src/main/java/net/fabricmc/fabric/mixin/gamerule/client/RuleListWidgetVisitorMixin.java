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

package net.fabricmc.fabric.mixin.gamerule.client;

/**
 * 1.18.2 Forge port placeholder.
 *
 * <p>The original mixin targeted the anonymous class
 * {@code EditGameRulesScreen$RuleListWidget$1} (visiting enum/double gamerule widgets with
 * FabricGameRuleVisitor). The legacy mixin annotation processor that Architectury Loom 1.3
 * hard-requires on the Forge platform crashes on anonymous-class targets
 * ({@code SignaturePrinter} / {@code StringIndexOutOfBoundsException}), so this mixin was
 * removed rather than ported.</p>
 *
 * <p>Impact: enum and double gamerules registered via fabric-game-rule-api-v1 fall back to the
 * default vanilla widgets and serialized names in the edit-gamerules screen instead of the
 * dedicated EnumRuleWidget/DoubleRuleWidget implementations.</p>
 */
final class RuleListWidgetVisitorMixin {
	private RuleListWidgetVisitorMixin() {
	}
}
