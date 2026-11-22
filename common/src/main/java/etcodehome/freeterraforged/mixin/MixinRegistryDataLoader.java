package etcodehome.freeterraforged.mixin;

import com.google.gson.JsonElement;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.Lifecycle;
import etcodehome.freeterraforged.world.worldgen.runtime.WorldgenSurfaceInputs;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.RegistryLoadTask;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceManagerRegistryLoadTask;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Map;

@Mixin(ResourceManagerRegistryLoadTask.class)
public abstract class MixinRegistryDataLoader<T> extends RegistryLoadTask<T> {

	protected MixinRegistryDataLoader(RegistryDataLoader.RegistryData<T> data, Lifecycle lifecycle, Map<ResourceKey<?>, Exception> loadingErrors) {
		super(data, lifecycle, loadingErrors);
	}

	@Shadow
	@Final
	private ResourceManager resourceManager;

	@WrapOperation(
		method = "lambda$load$2",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/resources/RegistryLoadTask$PendingRegistration;loadFromResource(Lcom/mojang/serialization/Decoder;Lnet/minecraft/resources/RegistryOps;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/server/packs/resources/Resource;)Lcom/mojang/datafixers/util/Either;"
		),
		require = 1
	)
	private Either<T, Exception> freeterraforged$acquireSurfaceInputs(
		Decoder<T> decoder, RegistryOps<JsonElement> ops, ResourceKey<T> key,
		Resource resource, Operation<Either<T, Exception>> original
	) throws java.io.IOException {
		if (this.registryKey().equals(Registries.NOISE_SETTINGS)) {
			resource = WorldgenSurfaceInputs.acquire(this.resourceManager, key.identifier(), resource);
		}
		return original.call(decoder, ops, key, resource);
	}
}
