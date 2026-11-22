package etcodehome.freeterraforged.platform;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import dev.architectury.injectables.annotations.ExpectPlatform;
import etcodehome.freeterraforged.FTFCommon;

public class ConfigUtil {
	public static final Path FTF_CONFIG_PATH = getConfigPath().resolve(FTFCommon.MOD_ID);
	public static final Path LEGACY_TF_CONFIG_PATH = getConfigPath().resolve(FTFCommon.LEGACY_TF_MOD_ID);
	public static final Path LEGACY_RTF_CONFIG_PATH = getConfigPath().resolve(FTFCommon.LEGACY_RTF_MOD_ID);

	public static Path ftf(String path) {
		return FTF_CONFIG_PATH.resolve(path);
	}

	public static Path legacy_tf(String path) {
		return LEGACY_TF_CONFIG_PATH.resolve(path);
	}
	public static Path legacy_rtf(String path) {
		return LEGACY_RTF_CONFIG_PATH.resolve(path);
	}

	@ExpectPlatform
	public static Path getConfigPath() {
		for (String pkg : new String[]{"fabric", "neoforge"}) {
			try {
				Class<?> impl = Class.forName("etcodehome.freeterraforged.platform." + pkg + ".ConfigUtilImpl");
				return (Path) impl.getMethod("getConfigPath").invoke(null);
			} catch (ReflectiveOperationException ignored) {
			}
		}
		return Paths.get("config");
	}

	static {
		if(!Files.exists(FTF_CONFIG_PATH)) {
			try {
				Files.createDirectories(FTF_CONFIG_PATH);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
