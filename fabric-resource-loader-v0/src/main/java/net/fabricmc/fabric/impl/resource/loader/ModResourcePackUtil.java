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

package net.fabricmc.fabric.impl.resource.loader;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Charsets;
import com.mojang.serialization.JsonOps;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.SharedConstants;
import net.minecraft.resource.DataPackSettings;
import net.minecraft.resource.LifecycledResourceManager;
import net.minecraft.resource.LifecycledResourceManagerImpl;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.resource.ResourcePackProfile;
import net.minecraft.resource.ResourceType;
import net.minecraft.resource.VanillaDataPackProvider;
import net.minecraft.util.dynamic.RegistryOps;
import net.minecraft.util.registry.DynamicRegistryManager;

import net.fabricmc.fabric.api.resource.ModResourcePack;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;

/**
 * Internal utilities for managing resource packs.
 */
public final class ModResourcePackUtil {
	private static final Logger LOGGER = LoggerFactory.getLogger("Fabric|ResourceLoader");

	private ModResourcePackUtil() {
	}

	/**
	 * Appends mod resource packs to the given list.
	 *
	 * @param packs   the resource pack list to append
	 * @param type    the type of resource
	 * @param subPath the resource pack sub path directory in mods, may be {@code null}
	 */
	public static void appendModResourcePacks(List<ModResourcePack> packs, ResourceType type, @Nullable String subPath) {
		for (ModContainer container : FabricLoader.getInstance().getAllMods()) {
			if (container.getMetadata().getType().equals("builtin")) {
				continue;
			}

			String modId = container.getMetadata().getId();

			if (isGameContainer(container, modId)) {
				LOGGER.info("Skipping mod resource pack of game/platform mod '{}' (root paths: {})", modId, container.getRootPaths());
				continue;
			}

			ModResourcePack pack = ModNioResourcePack.create(getName(container.getMetadata()), container, subPath, type, ResourcePackActivationType.ALWAYS_ENABLED);

			if (pack != null) {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("Mod resource pack for '{}' provides {} namespaces {} for {}", modId,
							pack.getNamespaces(type).size(), pack.getNamespaces(type), type);
				}

				packs.add(pack);
			} else if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Mod '{}' has no resources for {}, no pack was created (root paths: {})", modId, type, container.getRootPaths());
			}
		}
	}

	/**
	 * Checks whether the given mod container is really the game (or the platform it runs on) rather than a
	 * mod. Under Sinytra Connector the vanilla game and Forge are exposed as ordinary Fabric mods whose root
	 * paths are the game/platform jars. Game jars only ship <i>stub</i> copies of a handful of assets - most
	 * notably the title screen panoramas, which are 1x1 placeholder textures - because the real files are
	 * served by the asset index backed "Default" pack. Packaging those stubs into the always-on-top "Fabric
	 * Mods" pack would shadow the real assets, e.g. turning the main menu panorama into a flat grey square.
	 */
	private static boolean isGameContainer(ModContainer container, String modId) {
		if ("minecraft".equals(modId)) {
			return true;
		}

		// Game jars are marked with an assets/.mcassetsroot entry; no regular mod ships that marker.
		for (Path path : container.getRootPaths()) {
			try {
				if (Files.exists(path.resolve("assets/.mcassetsroot")) || Files.exists(path.resolve("data/.mcassetsroot"))) {
					return true;
				}
			} catch (Exception ignored) {
				// Invalid paths and closed file systems are simply not game jars.
			}
		}

		return false;
	}

	public static boolean containsDefault(ModMetadata info, String filename) {
		return "pack.mcmeta".equals(filename);
	}

	public static InputStream openDefault(ModMetadata info, ResourceType type, String filename) {
		switch (filename) {
		case "pack.mcmeta":
			String description = info.getName();

			if (description == null) {
				description = "";
			} else {
				description = description.replaceAll("\"", "\\\"");
			}

			String pack = String.format("{\"pack\":{\"pack_format\":" + type.getPackVersion(SharedConstants.getGameVersion()) + ",\"description\":\"%s\"}}", description);
			return IOUtils.toInputStream(pack, Charsets.UTF_8);
		default:
			return null;
		}
	}

	public static String getName(ModMetadata info) {
		if (info.getName() != null) {
			return info.getName();
		} else {
			return "Fabric Mod \"" + info.getId() + "\"";
		}
	}

	/**
	 * Creates the default data pack settings that replaces
	 * {@code DataPackSettings.SAFE_MODE} used in vanilla.
	 * @return the default data pack settings
	 */
	public static DataPackSettings createDefaultDataPackSettings() {
		ModResourcePackCreator modResourcePackCreator = new ModResourcePackCreator(ResourceType.SERVER_DATA);
		List<ResourcePackProfile> moddedResourcePacks = new ArrayList<>();
		modResourcePackCreator.register(moddedResourcePacks::add);

		List<String> enabled = new ArrayList<>(DataPackSettings.SAFE_MODE.getEnabled());
		List<String> disabled = new ArrayList<>(DataPackSettings.SAFE_MODE.getDisabled());

		// This ensures that any built-in registered data packs by mods which needs to be enabled by default are
		// as the data pack screen automatically put any data pack as disabled except the Default data pack.
		for (ResourcePackProfile profile : moddedResourcePacks) {
			ResourcePack pack = profile.createResourcePack();

			if (pack instanceof ModNioResourcePack && ((ModNioResourcePack) pack).getActivationType().isEnabledByDefault()) {
				enabled.add(profile.getName());
			} else {
				disabled.add(profile.getName());
			}
		}

		return new DataPackSettings(enabled, disabled);
	}

	public static void loadDynamicRegistry(DynamicRegistryManager.Mutable dynamicRegistryManager) {
		try (ResourcePackManager resourcePackManager = new ResourcePackManager(ResourceType.SERVER_DATA, new VanillaDataPackProvider(), new ModResourcePackCreator(ResourceType.SERVER_DATA))) {
			try (LifecycledResourceManager resourceManager = new LifecycledResourceManagerImpl(ResourceType.SERVER_DATA, resourcePackManager.createResourcePacks())) {
				RegistryOps.ofLoaded(JsonOps.INSTANCE, dynamicRegistryManager, resourceManager);
			}
		}
	}
}
