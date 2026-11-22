package etcodehome.freeterraforged.world.worldgen.feature;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.feature.Feature;
import etcodehome.freeterraforged.platform.RegistryUtil;
import etcodehome.freeterraforged.world.worldgen.feature.chance.ChanceFeature;
import etcodehome.freeterraforged.world.worldgen.feature.template.TemplateFeature;

public class FTFFeatures {
	public static final MapCodec<TemplateFeature> TEMPLATE = register("template", TemplateFeature.CODEC);
	public static final MapCodec<BushFeature> BUSH = register("bush", BushFeature.CODEC);
	public static final MapCodec<DiskFeature> DISK = register("disk", DiskFeature.CODEC);
	public static final MapCodec<ChanceFeature> CHANCE = register("chance", ChanceFeature.CODEC);
	public static final MapCodec<ScreeFeature> ERODE = register("erode", ScreeFeature.CODEC);
	public static final MapCodec<DecorateSnowFeature> DECORATE_SNOW = register("decorate_snow", DecorateSnowFeature.CODEC);
	public static final MapCodec<SwampSurfaceFeature> SWAMP_SURFACE = register("swamp_surface", SwampSurfaceFeature.CODEC);
	public static final MapCodec<RiverGasketFeature> RIVER_GASKET = register("river_gasket", RiverGasketFeature.CODEC);

	public static void bootstrap() {

	}

	private static <T extends Feature> MapCodec<T> register(String name, MapCodec<T> codec) {
		RegistryUtil.register(BuiltInRegistries.FEATURE_TYPE, name, codec);
		return codec;
	}
}
