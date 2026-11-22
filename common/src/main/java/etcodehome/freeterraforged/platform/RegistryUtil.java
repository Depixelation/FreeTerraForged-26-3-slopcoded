package etcodehome.freeterraforged.platform;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Lifecycle;
import dev.architectury.injectables.annotations.ExpectPlatform;
import etcodehome.freeterraforged.FTFCommon;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

/**
 * Placeholder class that uses Architecturies Impl framework to substitute a platform specific when compiling
 * @see etcodehome.freeterraforged.platform.neoforge.RegistryUtilImpl
 * @see etcodehome.freeterraforged.platform.fabric.RegistryUtilImpl
 */

public final class RegistryUtil {

	@ExpectPlatform
	public static <T> void register(Registry<T> registry, String name, T value) {
		for (String pkg : new String[]{"fabric", "neoforge"}) {
			try {
				Class<?> impl = Class.forName("etcodehome.freeterraforged.platform." + pkg + ".RegistryUtilImpl");
				impl.getMethod("register", Registry.class, String.class, Object.class).invoke(null, registry, name, value);
				return;
			} catch (ReflectiveOperationException ignored) {
			}
		}
		Registry.register(registry, FTFCommon.location(name), value);
	}

	@ExpectPlatform
	@SuppressWarnings("unchecked")
	public static <T> Registry<T> createRegistry(ResourceKey<Registry<T>> key) {
		for (String pkg : new String[]{"fabric", "neoforge"}) {
			try {
				Class<?> impl = Class.forName("etcodehome.freeterraforged.platform." + pkg + ".RegistryUtilImpl");
				return (Registry<T>) impl.getMethod("createRegistry", ResourceKey.class).invoke(null, key);
			} catch (ReflectiveOperationException ignored) {
			}
		}
		return new MappedRegistry<>(key, Lifecycle.stable());
	}

	@ExpectPlatform
	public static <T> void createDataRegistry(ResourceKey<Registry<T>> key, Codec<T> codec, boolean synced) {
		for (String pkg : new String[]{"fabric", "neoforge"}) {
			try {
				Class<?> impl = Class.forName("etcodehome.freeterraforged.platform." + pkg + ".RegistryUtilImpl");
				impl.getMethod("createDataRegistry", ResourceKey.class, Codec.class, boolean.class).invoke(null, key, codec, synced);
				return;
			} catch (ReflectiveOperationException ignored) {
			}
		}
	}
}
