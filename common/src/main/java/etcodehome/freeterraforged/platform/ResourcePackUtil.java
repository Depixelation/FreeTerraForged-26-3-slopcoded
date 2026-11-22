package etcodehome.freeterraforged.platform;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.server.packs.PackResources;

public final class ResourcePackUtil {
	private ResourcePackUtil() {}

	@ExpectPlatform
	public static boolean isBundledModPack(PackResources pack) {
		for (String pkg : new String[]{"fabric", "neoforge"}) {
			try {
				Class<?> impl = Class.forName("etcodehome.freeterraforged.platform." + pkg + ".ResourcePackUtilImpl");
				return (Boolean) impl.getMethod("isBundledModPack", PackResources.class).invoke(null, pack);
			} catch (ReflectiveOperationException ignored) {
			}
		}
		return false;
	}
}
