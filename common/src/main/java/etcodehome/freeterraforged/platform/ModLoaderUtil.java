package etcodehome.freeterraforged.platform;

import java.util.Optional;

import dev.architectury.injectables.annotations.ExpectPlatform;

public class ModLoaderUtil {
	
	@ExpectPlatform
	public static boolean isLoaded(String modId) {
		for (String pkg : new String[]{"fabric", "neoforge"}) {
			try {
				Class<?> impl = Class.forName("etcodehome.freeterraforged.platform." + pkg + ".ModLoaderUtilImpl");
				return (Boolean) impl.getMethod("isLoaded", String.class).invoke(null, modId);
			} catch (ReflectiveOperationException ignored) {
			}
		}
		return false;
	}

	@ExpectPlatform
	public static Optional<String> version(String modId) {
		for (String pkg : new String[]{"fabric", "neoforge"}) {
			try {
				Class<?> impl = Class.forName("etcodehome.freeterraforged.platform." + pkg + ".ModLoaderUtilImpl");
				@SuppressWarnings("unchecked")
				Optional<String> res = (Optional<String>) impl.getMethod("version", String.class).invoke(null, modId);
				return res;
			} catch (ReflectiveOperationException ignored) {
			}
		}
		return Optional.empty();
	}
}
