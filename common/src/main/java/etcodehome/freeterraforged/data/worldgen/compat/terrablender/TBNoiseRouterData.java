package etcodehome.freeterraforged.data.worldgen.compat.terrablender;

import etcodehome.freeterraforged.world.worldgen.densityfunction.CellSampler;
import etcodehome.freeterraforged.world.worldgen.densityfunction.FTFDensityFunctions;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;

public class TBNoiseRouterData {
	public static final ResourceKey<DensityFunction> UNIQUENESS =ResourceKey.create(Registries.DENSITY_FUNCTION, Identifier.fromNamespaceAndPath("terrablender","uniqueness"));
	
	public static void bootstrap(BootstrapContext<DensityFunction> ctx) {
		ctx.register(UNIQUENESS, FTFDensityFunctions.cell(CellSampler.Field.BIOME_REGION));
	}
}
