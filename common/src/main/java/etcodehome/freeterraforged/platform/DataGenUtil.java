package etcodehome.freeterraforged.platform;

import java.util.concurrent.CompletableFuture;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

public final class DataGenUtil {

	@ExpectPlatform
	public static DataProvider createRegistryProvider(PackOutput output, CompletableFuture<RegistrySetBuilder.PatchedRegistries> registries) {
		for (String pkg : new String[]{"fabric", "neoforge"}) {
			try {
				Class<?> impl = Class.forName("etcodehome.freeterraforged.platform." + pkg + ".DataGenUtilImpl");
				return (DataProvider) impl.getMethod("createRegistryProvider", PackOutput.class, CompletableFuture.class).invoke(null, output, registries);
			} catch (ClassNotFoundException | NoSuchMethodException ignored) {
			} catch (ReflectiveOperationException ex) {
				Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
				if (cause instanceof RuntimeException re) throw re;
				throw new RuntimeException(cause);
			}
		}
		throw new IllegalStateException("No platform DataGenUtilImpl found");
	}
}
