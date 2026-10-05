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

package net.fabricmc.fabric.impl.sinytra;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.stream.Stream;

import net.minecraft.resource.DirectoryResourcePack;
import net.minecraft.resource.ResourcePackProfile;
import net.minecraft.resource.ResourcePackProvider;
import net.minecraft.resource.metadata.PackResourceMetadata;

import net.minecraft.resource.ResourcePackSource;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("fabric_api")
public class SinytraFabric {
	public SinytraFabric() {
		FMLJavaModLoadingContext.get().getModEventBus().addListener(EventPriority.NORMAL, false, AddPackFindersEvent.class, this::addPackFinder);
	}

	/**
	 * Some mods (ARRP) may use a resource pack named {@code fabric} to sort their own packs around. So just provide it.
	 *
	 * <p>1.18.2 note: {@code RepositorySource} (here named {@code ResourcePackProvider}) takes two
	 * parameters in 1.18.2 — the profile consumer plus a profile factory — and
	 * {@code ResourcePackSource.BUILTIN} is spelled {@code PACK_SOURCE_BUILTIN}. The factory's
	 * seven-argument overload is a default method on the Forge-patched interface, so calling it
	 * from an anonymous implementation is fine.
	 */
	private void addPackFinder(AddPackFindersEvent event) {
		event.addRepositorySource(new ResourcePackProvider() {
			@Override
			public void register(Consumer<ResourcePackProfile> profileAdder, ResourcePackProfile.Factory factory) {
				File dummy = resolveDummyPackDir();
				profileAdder.accept(factory.create(
						"fabric",
						Text.of("fabric"),
						true,
						() -> new DirectoryResourcePack(dummy),
						new PackResourceMetadata(new LiteralText("Fabric API compatibility pack"), 8),
						ResourcePackProfile.InsertionPosition.TOP,
						ResourcePackSource.PACK_SOURCE_BUILTIN
				));
			}
		});
	}

	/**
	 * 1.18.2 port note: under the Connector bootstrap the mod jar is mounted on a non-default
	 * (union / zipfs) file system, so {@link Path#toFile()} throws
	 * {@link UnsupportedOperationException} ("Path not associated with default file system").
	 * When that happens, materialise the bundled {@code dummyrp} directory into a real temporary
	 * directory and hand that to {@link DirectoryResourcePack} instead.
	 */
	private static File resolveDummyPackDir() {
		Path source = ModList.get().getModContainerById("fabric_api").get().getModInfo().getOwningFile()
				.getFile().findResource("dummyrp");

		if (source.getFileSystem() == FileSystems.getDefault()) {
			return source.toFile();
		}

		try {
			Path target = Files.createTempDirectory("fabric-api-dummyrp");
			target.toFile().deleteOnExit();

			if (Files.exists(source)) {
				try (Stream<Path> paths = Files.walk(source)) {
					for (Path path : (Iterable<Path>) paths::iterator) {
						Path destination = target.resolve(source.relativize(path).toString());

						if (Files.isDirectory(path)) {
							Files.createDirectories(destination);
						} else {
							Files.createDirectories(destination.getParent());
							Files.copy(path, destination);
						}
					}
				}
			}

			return target.toFile();
		} catch (IOException e) {
			throw new RuntimeException("Fabric API: failed to materialise the dummy resource pack", e);
		}
	}
}
