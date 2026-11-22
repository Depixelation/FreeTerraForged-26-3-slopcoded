package etcodehome.freeterraforged.platform.fabric;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.registries.RegistriesDatapackGenerator;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.ResourceKey;

public class DataGenUtilImpl {

	public static DataProvider createRegistryProvider(PackOutput output, CompletableFuture<RegistrySetBuilder.PatchedRegistries> registries) {
		Set<ResourceKey<? extends Registry<?>>> emitted = new HashSet<>();
		List<RegistryDataLoader.RegistryData<?>> registryData = Stream.concat(
			RegistryDataLoader.WORLD_REGISTRIES.stream(),
			DynamicRegistries.getAllDynamicRegistries().stream()
		).filter(registry -> emitted.add(registry.key())).toList();

		return new RegistriesDatapackGenerator(output, "Registries", registryData, registries.thenApply(RegistrySetBuilder.PatchedRegistries::patches));
	}
}
