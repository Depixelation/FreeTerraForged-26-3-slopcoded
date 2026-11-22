package etcodehome.freeterraforged.data.worldgen.preset;

import etcodehome.freeterraforged.FTFCommon;
import etcodehome.freeterraforged.data.worldgen.preset.settings.CaveSettings;
import etcodehome.freeterraforged.data.worldgen.preset.settings.Preset;
import etcodehome.freeterraforged.data.worldgen.preset.settings.WorldSettings;
import etcodehome.freeterraforged.world.worldgen.densityfunction.CellSampler;
import etcodehome.freeterraforged.world.worldgen.densityfunction.FTFDensityFunctions;
import etcodehome.freeterraforged.world.worldgen.noise.module.Noise;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.OverworldBiomeBuilder;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.NoiseRouterData;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunctions;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public class PresetNoiseRouterData {
	public static final ResourceKey<DensityFunction> HEIGHT = createKey("height");
	public static final ResourceKey<DensityFunction> GRADIENT = createKey("gradient");
	public static final ResourceKey<DensityFunction> HEIGHT_EROSION = createKey("erosion");
	public static final ResourceKey<DensityFunction> SEDIMENT = createKey("sediment");
	private static final ResourceKey<DensityFunction> VANILLA_SHIFT_X = createMinecraftKey("shift_x");
	private static final ResourceKey<DensityFunction> VANILLA_SHIFT_Z = createMinecraftKey("shift_z");
	
	private static final float SCALER = 128.0F;
	private static final float UNIT = 1.0F / SCALER;
	private static final float SURFACE_CLIMATE_DEPTH = 0.03F;
	private static final float UNDERGROUND_CLIMATE_DEPTH = 0.125F;
	private static final float UNDERGROUND_EROSION_VARIATION = 0.25F;
	
    public static void bootstrap(Preset preset, BootstrapContext<DensityFunction> ctx) {
        HolderGetter<DensityFunction> densityFunctions = ctx.lookup(Registries.DENSITY_FUNCTION);
        HolderGetter<NormalNoise> noiseParams = ctx.lookup(Registries.NOISE);
        
    	WorldSettings worldSettings = preset.world();
    	WorldSettings.Properties properties = worldSettings.properties;
    	
    	CaveSettings caveSettings = preset.caves();
    	
        int worldHeight = properties.worldHeight;
        int worldDepth = properties.worldDepth;
        
        ctx.register(NoiseRouterData.OVERWORLD_FUNCTIONS.continents(), FTFDensityFunctions.cell(CellSampler.Field.CONTINENT));
        ctx.register(NoiseRouterData.OVERWORLD_FUNCTIONS.erosion(), FTFDensityFunctions.cell(CellSampler.Field.EROSION));
        DensityFunction ridges = FTFDensityFunctions.cell(CellSampler.Field.WEIRDNESS);
        ctx.register(NoiseRouterData.RIDGES, ridges);
        ctx.register(NoiseRouterData.RIDGES_FOLDED, NoiseRouterData.peaksAndValleys(ridges));

        DensityFunction height = NoiseRouterData.registerAndWrap(ctx, HEIGHT, FTFDensityFunctions.cell(CellSampler.Field.HEIGHT));
        DensityFunction offset = NoiseRouterData.registerAndWrap(ctx, NoiseRouterData.OVERWORLD_FUNCTIONS.offset(), DensityFunctions.add(DensityFunctions.constant(-1.0F), DensityFunctions.mul(height, DensityFunctions.constant(2.0F))));
        DensityFunction terrainDensity = DensityFunctions.add(DensityFunctions.yClampedGradient(-worldDepth, worldHeight, yGradientRange(-worldDepth), yGradientRange(worldHeight)), offset);
        DensityFunction factor = NoiseRouterData.getFunction(densityFunctions, NoiseRouterData.OVERWORLD_FUNCTIONS.factor());
        DensityFunction slopedCheese = NoiseRouterData.registerAndWrap(ctx, NoiseRouterData.OVERWORLD_FUNCTIONS.slopedCheese(), NoiseRouterData.noiseGradientDensity(factor, terrainDensity));
        ctx.register(NoiseRouterData.OVERWORLD_FUNCTIONS.depth(), FTFDensityFunctions.climateDepth(worldDepth, worldHeight));
        ctx.register(NoiseRouterData.BASE_3D_NOISE_OVERWORLD, DensityFunctions.zero());
        ctx.register(NoiseRouterData.OVERWORLD_FUNCTIONS.jaggedness(), jaggednessPerformanceHack());

        DensityFunction preliminarySurfaceLevel = NoiseRouterData.registerAndWrap(
            ctx,
            NoiseRouterData.OVERWORLD_FUNCTIONS.preliminarySurfaceLevel(),
            DensityFunctions.add(
                DensityFunctions.mul(height, DensityFunctions.constant((float) properties.terrainScaleFactor())),
                DensityFunctions.constant(-10.0F)
            )
        );
        ctx.register(
            NoiseRouterData.OVERWORLD_FUNCTIONS.chunkSurfaceLevel(),
            DensityFunctions.interpolated(preliminarySurfaceLevel, 16, 1)
        );
                
        ctx.register(NoiseRouterData.NOODLE, noodle(-worldDepth, worldHeight, 1.0F - caveSettings.noodleCaveProbability, densityFunctions, noiseParams));
        ctx.register(NoiseRouterData.ENTRANCES, caveSettings.entranceCaveProbability > 0.0F
            ? NoiseRouterData.entrances(densityFunctions, noiseParams)
            : DensityFunctions.constant(1.0F));
        ctx.register(NoiseRouterData.SPAGHETTI_2D, probabilityDensity(caveSettings.spaghettiCaveProbability, spaghetti2D(-worldDepth, worldHeight, densityFunctions, noiseParams)));
        
        ctx.register(GRADIENT, FTFDensityFunctions.cell(CellSampler.Field.GRADIENT));
        ctx.register(HEIGHT_EROSION, FTFDensityFunctions.cell(CellSampler.Field.HEIGHT_EROSION));
        ctx.register(SEDIMENT, FTFDensityFunctions.cell(CellSampler.Field.SEDIMENT));
    }
    
    protected static NoiseRouter overworld(Preset preset, HolderGetter<DensityFunction> densityFunctions, HolderGetter<NormalNoise> noiseParams, HolderGetter<Noise> noises) {
    	WorldSettings worldSettings = preset.world();
    	WorldSettings.Properties properties = worldSettings.properties;
    	int worldDepth = properties.worldDepth;
    	int worldHeight = properties.worldHeight;
    	
    	CaveSettings caves = preset.caves();
    	float cheeseCaveDepthOffset = caves.cheeseCaveDepthOffset;
        DensityFunction factor = NoiseRouterData.getFunction(densityFunctions, NoiseRouterData.OVERWORLD_FUNCTIONS.factor());
        DensityFunction depth = NoiseRouterData.getFunction(densityFunctions, NoiseRouterData.OVERWORLD_FUNCTIONS.depth());
        DensityFunction offset = NoiseRouterData.getFunction(densityFunctions, NoiseRouterData.OVERWORLD_FUNCTIONS.offset());
        DensityFunction terrainDensity = DensityFunctions.add(
            DensityFunctions.yClampedGradient(-worldDepth, worldHeight, yGradientRange(-worldDepth), yGradientRange(worldHeight)),
            offset
        );
        DensityFunction temperature = FTFDensityFunctions.cell(CellSampler.Field.TEMPERATURE);
        DensityFunction vegetation = FTFDensityFunctions.cell(CellSampler.Field.MOISTURE);
        DensityFunction continents = NoiseRouterData.getFunction(densityFunctions, NoiseRouterData.OVERWORLD_FUNCTIONS.continents());
        DensityFunction erosion = NoiseRouterData.getFunction(densityFunctions, NoiseRouterData.OVERWORLD_FUNCTIONS.erosion());
        DensityFunction ridges = NoiseRouterData.getFunction(densityFunctions, NoiseRouterData.RIDGES);
        DensityFunction initialDensity = NoiseRouterData.noiseGradientDensity(factor, terrainDensity);
        DensityFunction slopedCheese = initialDensity;

        DensityFunction entrances;
        if (caves.entranceCaveProbability > 0.0F) {
            DensityFunction rawEntrances = DensityFunctions.interpolated(
                NoiseRouterData.getFunction(densityFunctions, NoiseRouterData.ENTRANCES),
                4,
                8
            );
            DensityFunction connectedEntrances = DensityFunctions.rangeChoice(
                rawEntrances,
                -1000000.0F,
                -0.3125F,
                rawEntrances,
                DensityFunctions.constant(1.0F)
            );
            entrances = DensityFunctions.min(slopedCheese, DensityFunctions.mul(DensityFunctions.constant(5.0F), connectedEntrances));
        } else {
            entrances = slopedCheese;
        }

        DensityFunction slopedCheeseRange = DensityFunctions.mul(DensityFunctions.rangeChoice(slopedCheese, -1000000.0F, cheeseCaveDepthOffset, entrances, DensityFunctions.interpolated(slideOverworld(underground(caves.cheeseCaveProbability, densityFunctions, noiseParams, slopedCheese), -worldDepth), 4, 8)), DensityFunctions.constant(0.64F)).squeeze();
        DensityFunction finalDensity = DensityFunctions.add(
            DensityFunctions.min(slopedCheeseRange, NoiseRouterData.getFunction(densityFunctions, NoiseRouterData.NOODLE)),
            DensityFunctions.beardifier()
        );
        DensityFunction chunkSurfaceLevel = NoiseRouterData.getFunction(densityFunctions, NoiseRouterData.OVERWORLD_FUNCTIONS.chunkSurfaceLevel());
        return new NoiseRouter(temperature, vegetation, continents, erosion, depth, ridges, chunkSurfaceLevel, finalDensity);
	}

    protected static Aquifer.Config overworldAquifers(HolderGetter<DensityFunction> densityFunctions, HolderGetter<NormalNoise> noiseParams) {
        DensityFunction aquiferBarrier = DensityFunctions.noise(noiseParams.getOrThrow(Noises.AQUIFER_BARRIER), 0.5);
        DensityFunction aquiferFluidLevelFloodedness = DensityFunctions.noise(noiseParams.getOrThrow(Noises.AQUIFER_FLUID_LEVEL_FLOODEDNESS), 0.67);
        DensityFunction aquiferFluidLevelSpread = DensityFunctions.noise(noiseParams.getOrThrow(Noises.AQUIFER_FLUID_LEVEL_SPREAD), 0.7142857142857143);
        DensityFunction aquiferLava = DensityFunctions.noise(noiseParams.getOrThrow(Noises.AQUIFER_LAVA));
        DensityFunction erosion = NoiseRouterData.getFunction(densityFunctions, NoiseRouterData.OVERWORLD_FUNCTIONS.erosion());
        DensityFunction depth = NoiseRouterData.getFunction(densityFunctions, NoiseRouterData.OVERWORLD_FUNCTIONS.depth());
        DensityFunction deepDark = OverworldBiomeBuilder.deepDarkRegion(erosion, depth);
        DensityFunction surfaceLevel = NoiseRouterData.getFunction(densityFunctions, NoiseRouterData.OVERWORLD_FUNCTIONS.preliminarySurfaceLevel());
        return new Aquifer.Config(aquiferBarrier, aquiferFluidLevelFloodedness, aquiferFluidLevelSpread, aquiferLava, deepDark, surfaceLevel);
    }

    private static DensityFunction blendClimateAxis(DensityFunction depth, DensityFunction surface, DensityFunction underground) {
        float transitionRange = UNDERGROUND_CLIMATE_DEPTH - SURFACE_CLIMATE_DEPTH;
        DensityFunction alpha = DensityFunctions.mul(
            DensityFunctions.add(depth, DensityFunctions.constant(-SURFACE_CLIMATE_DEPTH)),
            DensityFunctions.constant(1.0F / transitionRange)
        ).clamp(0.0F, 1.0F);
        return DensityFunctions.lerp(alpha, surface, underground);
    }

    private static DensityFunction terrainAlignedUndergroundContinentalness(
        WorldSettings worldSettings,
        DensityFunction surfaceContinents,
        DensityFunction vanillaContinents
    ) {
        float squeezeLimit = 11.0F / 24.0F;
        DensityFunction unitNoise = DensityFunctions.mul(
            DensityFunctions.add(vanillaContinents.squeeze(), DensityFunctions.constant(squeezeLimit)),
            DensityFunctions.constant(1.0F / (squeezeLimit * 2.0F))
        );
        DensityFunction inland = DensityFunctions.add(
            DensityFunctions.constant(-0.11F),
            DensityFunctions.mul(DensityFunctions.constant(1.11F), unitNoise.square())
        );
        return blendCoastToInland(worldSettings, surfaceContinents, inland);
    }

    private static DensityFunction terrainAlignedUndergroundErosion(
        WorldSettings worldSettings,
        DensityFunction terrainErosion,
        DensityFunction vanillaErosion
    ) {
        DensityFunction landErosion = DensityFunctions.add(
            terrainErosion,
            DensityFunctions.mul(DensityFunctions.constant(UNDERGROUND_EROSION_VARIATION), vanillaErosion)
        );
        return blendCoastToInland(worldSettings, vanillaErosion, landErosion);
    }

    private static DensityFunction blendCoastToInland(
        WorldSettings worldSettings,
        DensityFunction coast,
        DensityFunction inland
    ) {
        WorldSettings.ControlPoints controlPoints = worldSettings.controlPoints;
        float transitionRange = controlPoints.inland - controlPoints.beach;
        DensityFunction alpha = DensityFunctions.mul(
            DensityFunctions.add(
                FTFDensityFunctions.cell(CellSampler.Field.CONTINENT_EDGE),
                DensityFunctions.constant(-controlPoints.beach)
            ),
            DensityFunctions.constant(1.0F / transitionRange)
        ).clamp(0.0F, 1.0F);
        return DensityFunctions.lerp(
            alpha,
            coast,
            inland
        );
    }

    private static DensityFunction underground(float cheeseCaveProbability, HolderGetter<DensityFunction> densityFunctions, HolderGetter<NormalNoise> noiseParams, DensityFunction slopedCheese) {
        DensityFunction spaghetti2d = NoiseRouterData.getFunction(densityFunctions, NoiseRouterData.SPAGHETTI_2D);
        DensityFunction spaghettiRoughnessFunction = NoiseRouterData.getFunction(densityFunctions, NoiseRouterData.SPAGHETTI_ROUGHNESS_FUNCTION);
        DensityFunction caveLayerNoise = DensityFunctions.noise(noiseParams.getOrThrow(Noises.CAVE_LAYER), 8.0);
        DensityFunction caveLayer = DensityFunctions.mul(DensityFunctions.constant(4.0F), caveLayerNoise.square());
        DensityFunction caveCheese = probabilityDensity(cheeseCaveProbability, DensityFunctions.noise(noiseParams.getOrThrow(Noises.CAVE_CHEESE), 0.6666666666666666));
        DensityFunction slopedCaves = DensityFunctions.add(DensityFunctions.add(DensityFunctions.constant(0.27F), caveCheese).clamp(-1.0F, 1.0F), DensityFunctions.add(DensityFunctions.constant(1.5F), DensityFunctions.mul(DensityFunctions.constant(-0.64F), slopedCheese)).clamp(0.0F, 0.5F));
        DensityFunction slopedCaveLayered = DensityFunctions.add(caveLayer, slopedCaves);
        DensityFunction underground = DensityFunctions.min(DensityFunctions.min(slopedCaveLayered, NoiseRouterData.getFunction(densityFunctions, NoiseRouterData.ENTRANCES)), DensityFunctions.add(spaghetti2d, spaghettiRoughnessFunction));
        DensityFunction pillars = NoiseRouterData.getFunction(densityFunctions, NoiseRouterData.PILLARS);
        DensityFunction pillarRange = DensityFunctions.rangeChoice(pillars, -1000000.0F, 0.03F, DensityFunctions.constant(-1000000.0F), pillars);
        return DensityFunctions.max(underground, pillarRange);
    }

    private static DensityFunction spaghetti2D(int minY, int maxY, HolderGetter<DensityFunction> densityFunctions, HolderGetter<NormalNoise> noiseParams) {
        DensityFunction modulator = DensityFunctions.noise(noiseParams.getOrThrow(Noises.SPAGHETTI_2D_MODULATOR), 2.0, 1.0);
        DensityFunction sampler = wrapRarity2d(modulator, noiseParams.getOrThrow(Noises.SPAGHETTI_2D));
        DensityFunction elevation = DensityFunctions.mappedNoise(noiseParams.getOrThrow(Noises.SPAGHETTI_2D_ELEVATION), 0.0, (float) Math.floorDiv(minY, 8), 8.0F);
        DensityFunction thicknessModulator = NoiseRouterData.getFunction(densityFunctions, NoiseRouterData.SPAGHETTI_2D_THICKNESS_MODULATOR);
        DensityFunction elevationGradient = DensityFunctions.add(DensityFunctions.cache(elevation), DensityFunctions.yClampedGradient(minY, maxY, minY / -8.0F, maxY / -8.0F)).abs();
        DensityFunction normal = DensityFunctions.add(elevationGradient, thicknessModulator).cube();
        DensityFunction weird = DensityFunctions.add(sampler, DensityFunctions.mul(DensityFunctions.constant(0.083F), thicknessModulator));
        return DensityFunctions.max(weird, normal).clamp(-1.0F, 1.0F);
    }

    private static DensityFunction wrapRarity2d(DensityFunction modulator, net.minecraft.core.Holder<NormalNoise> noise) {
        return DensityFunctions.intervalSelect(
            modulator,
            it.unimi.dsi.fastutil.floats.FloatList.of(-0.75F, -0.5F, 0.5F, 0.75F),
            java.util.List.of(
                noiseFunctionForRarity(noise, 0.5F),
                noiseFunctionForRarity(noise, 0.75F),
                noiseFunctionForRarity(noise, 1.0F),
                noiseFunctionForRarity(noise, 2.0F),
                noiseFunctionForRarity(noise, 3.0F)
            )
        ).abs();
    }

    private static DensityFunction noiseFunctionForRarity(net.minecraft.core.Holder<NormalNoise> noise, float rarity) {
        return DensityFunctions.noise(noise, 1.0D / rarity, 1.0D / rarity).mul(rarity);
    }

    private static DensityFunction noodle(int minY, int maxY, float threshold, HolderGetter<DensityFunction> densityFunctions, HolderGetter<NormalNoise> noiseParams) {
    	int baseY = minY + 4;
        
    	DensityFunction y = NoiseRouterData.getFunction(densityFunctions, NoiseRouterData.Y);
        DensityFunction selector = NoiseRouterData.yLimitedInterpolatable(y, DensityFunctions.noise(noiseParams.getOrThrow(Noises.NOODLE), 1.0, 1.0), baseY, maxY, -1, 4, 8);
        DensityFunction thickness = NoiseRouterData.yLimitedInterpolatable(y, DensityFunctions.mappedNoise(noiseParams.getOrThrow(Noises.NOODLE_THICKNESS), 1.0, 1.0, -0.05F, -0.1F), baseY, maxY, 0, 4, 8);
        DensityFunction ridgeA = NoiseRouterData.yLimitedInterpolatable(y, DensityFunctions.noise(noiseParams.getOrThrow(Noises.NOODLE_RIDGE_A), 2.6666666666666665, 2.6666666666666665), baseY, maxY, 0, 4, 8);
        DensityFunction ridgeB = NoiseRouterData.yLimitedInterpolatable(y, DensityFunctions.noise(noiseParams.getOrThrow(Noises.NOODLE_RIDGE_B), 2.6666666666666665, 2.6666666666666665), baseY, maxY, 0, 4, 8);
        DensityFunction ridge = DensityFunctions.mul(DensityFunctions.constant(1.5F), DensityFunctions.max(ridgeA.abs(), ridgeB.abs()));
        return DensityFunctions.rangeChoice(selector, -1000000.0F, threshold, DensityFunctions.constant(64.0F), DensityFunctions.add(thickness, ridge));
    }
    
    private static DensityFunction slideOverworld(DensityFunction function, int minY) {
        return slide(function, minY, 0, 24, UNIT * 15);
    }
    
    private static DensityFunction slide(DensityFunction function, int minY, int bottomGradientStart, int bottomGradientEnd, float bottomGradientTarget) {
        DensityFunction bottomGradient = DensityFunctions.yClampedGradient(minY + bottomGradientStart, minY + bottomGradientEnd, 0.0F, 1.0F);
        return DensityFunctions.lerp(bottomGradient, bottomGradientTarget, function);
    }
    
    /* 
     * the multiply function doesnt sample the second input
     * if the first input is zero, however this optimization doesn't get
     * applied if either input is a Constant, so if we use 
     * use DensityFunctions.zero() Noises.JAGGED will still get sampled
     */
    private static DensityFunction jaggednessPerformanceHack() {
    	return DensityFunctions.add(DensityFunctions.zero(), DensityFunctions.zero());
    }

    // do this a different way, since this affects the size of the cave as well
    @Deprecated
    private static DensityFunction probabilityDensity(float probability, DensityFunction function) {
    	if(probability == 0.0F) {
    		return DensityFunctions.constant(1.0F);
    	}
    	return DensityFunctions.add(DensityFunctions.constant(1.0F - probability), function);
    }
    
    private static float yGradientRange(float range) {
    	return 1.0F + (-range / SCALER);
    }
    
    private static ResourceKey<DensityFunction> createKey(String string) {
        return ResourceKey.create(Registries.DENSITY_FUNCTION, FTFCommon.location(string));
    }

    private static ResourceKey<DensityFunction> createMinecraftKey(String string) {
        return ResourceKey.create(Registries.DENSITY_FUNCTION, Identifier.withDefaultNamespace(string));
    }
}
