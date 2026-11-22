package etcodehome.freeterraforged.data.worldgen.preset;

import java.util.List;
import java.util.Optional;

import etcodehome.freeterraforged.data.worldgen.preset.settings.Preset;
import etcodehome.freeterraforged.data.worldgen.preset.settings.WorldSettings;
import etcodehome.freeterraforged.registries.FTFRegistries;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.NoiseRouterData;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.material.rule.MaterialRule;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import etcodehome.freeterraforged.world.worldgen.noise.module.Noise;

public class PresetNoiseGeneratorSettings {
	
	public static void bootstrap(Preset preset, BootstrapContext<NoiseGeneratorSettings> ctx) {
		HolderGetter<DensityFunction> densityFunctions = ctx.lookup(Registries.DENSITY_FUNCTION);
		HolderGetter<NormalNoise> noiseParams = ctx.lookup(Registries.NOISE);
		HolderGetter<MaterialRule> materialRules = ctx.lookup(Registries.MATERIAL_RULE);
		HolderGetter<Noise> noises = ctx.lookup(FTFRegistries.NOISE);
		
		WorldSettings worldSettings = preset.world();
		WorldSettings.Properties properties = worldSettings.properties;
		int worldHeight = properties.worldHeight;
		int worldDepth = properties.worldDepth;

		NoiseRouter noiseRouter = PresetNoiseRouterData.overworld(preset, densityFunctions, noiseParams, noises);
		NoiseGeneratorSettings.DebugFunctions debugFunctions = new NoiseGeneratorSettings.DebugFunctions(List.of(
			new NoiseGeneratorSettings.DebugFunctionEntry("N", noiseRouter.finalDensity()),
			new NoiseGeneratorSettings.DebugFunctionEntry("T", noiseRouter.temperature()),
			new NoiseGeneratorSettings.DebugFunctionEntry("V", noiseRouter.vegetation()),
			new NoiseGeneratorSettings.DebugFunctionEntry("C", noiseRouter.continents()),
			new NoiseGeneratorSettings.DebugFunctionEntry("E", noiseRouter.erosion()),
			new NoiseGeneratorSettings.DebugFunctionEntry("D", noiseRouter.depth()),
			new NoiseGeneratorSettings.DebugFunctionEntry("W", noiseRouter.ridges()),
			new NoiseGeneratorSettings.DebugFunctionEntry("PV", NoiseRouterData.peaksAndValleys(noiseRouter.ridges())),
			new NoiseGeneratorSettings.DebugFunctionEntry("PS", NoiseRouterData.getFunction(densityFunctions, NoiseRouterData.OVERWORLD_FUNCTIONS.preliminarySurfaceLevel()))
		));

		ctx.register(NoiseGeneratorSettings.OVERWORLD, new NoiseGeneratorSettings(
			NoiseSettings.create(-worldDepth, worldDepth + worldHeight), 
			Blocks.STONE.defaultBlockState(), Blocks.WATER.defaultBlockState(), 
			noiseRouter,
			Holder.direct(PresetSurfaceRuleData.overworld(preset, materialRules, noises)),
			properties.spawnType.getSpawnTargets(densityFunctions), 
			properties.seaLevel, 
			false, 
			Optional.of(PresetNoiseRouterData.overworldAquifers(densityFunctions, noiseParams)), 
			false,
			debugFunctions
		));
    }
}
