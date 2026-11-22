package etcodehome.freeterraforged.world.worldgen.floatproviders;

import com.mojang.serialization.MapCodec;
import etcodehome.freeterraforged.platform.RegistryUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.valueproviders.FloatProvider;

public class FTFFloatProviderTypes {
	public static final MapCodec<LegacyCanyonYScale> LEGACY_CANYON_Y_SCALE = register("legacy_canyon_y_scale", LegacyCanyonYScale.CODEC);
	
	public static void bootstrap() {
	}
	
	private static <T extends FloatProvider> MapCodec<T> register(String name, MapCodec<T> codec) {
		RegistryUtil.register(BuiltInRegistries.FLOAT_PROVIDER_TYPE, name, codec);
		return codec;
	}
}
