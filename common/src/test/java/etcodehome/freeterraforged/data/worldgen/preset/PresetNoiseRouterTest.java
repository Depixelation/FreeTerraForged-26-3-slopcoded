package etcodehome.freeterraforged.data.worldgen.preset;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.NoiseRouterData;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

import etcodehome.freeterraforged.data.worldgen.preset.settings.CaveSettings;
import etcodehome.freeterraforged.data.worldgen.preset.settings.Preset;
import etcodehome.freeterraforged.data.worldgen.preset.settings.Presets;
import etcodehome.freeterraforged.registries.FTFRegistries;
import etcodehome.freeterraforged.world.worldgen.FTFRandomState;
import etcodehome.freeterraforged.world.worldgen.GeneratorContext;
import etcodehome.freeterraforged.world.worldgen.cell.Cell;
import etcodehome.freeterraforged.world.worldgen.densityfunction.CellSampler;
import etcodehome.freeterraforged.world.worldgen.noise.module.Noise;

import static org.junit.jupiter.api.Assertions.*;

public class PresetNoiseRouterTest {

    @BeforeAll
    static void setUp() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void testCaveSettingsDefaultEntranceProbabilityIsZero() {
        assertEquals(0.0F, CaveSettings.makeDefault().entranceCaveProbability, "Default entrance cave probability must be 0.0F");
        assertEquals(0.0F, Presets.makeFTFDefault().caves().entranceCaveProbability, "Default FTF preset entrance cave probability must be 0.0F");
    }

    @Test
    void testDefaultPresetDensitiesAtOceanFloor() {
        long seed = -9047628570632126792L;

        Preset preset = Presets.makeFTFDefault();

        RegistryAccess.Frozen registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        HolderLookup.Provider provider = preset.buildPreviewLookups(registries);
        HolderGetter<Noise> noises = provider.lookupOrThrow(FTFRegistries.NOISE);

        GeneratorContext ctx = GeneratorContext.make(preset, noises, seed);
        int seaLevel = preset.world().properties.seaLevel;

        // Sample a grid to locate ocean floor cells and verify Bug A fix:
        // preliminarySurfaceLevel + 8 must be strictly below seaLevel so Aquifer doesn't drain ocean
        int oceanCellsChecked = 0;
        for (int x = -4096; x <= 4096; x += 256) {
            for (int z = -4096; z <= 4096; z += 256) {
                Cell cell = ctx.lookup.lookup(x, z);
                int scaledY = ctx.levels.scale(cell.height);
                if (scaledY < seaLevel) {
                    oceanCellsChecked++;
                    float preliminarySurfaceLevel = (cell.height * preset.world().properties.terrainScaleFactor()) - 10.0F;
                    float adjustedSurfaceLevel = preliminarySurfaceLevel + 8.0F;
                    assertTrue(adjustedSurfaceLevel < seaLevel,
                        "adjustedSurfaceLevel (" + adjustedSurfaceLevel + ") must be < seaLevel (" + seaLevel
                            + ") at (" + x + ", " + z + ") to prevent aquifer ocean drainage");
                }
            }
        }
        assertTrue(oceanCellsChecked > 0, "Expected to find at least one ocean floor cell in sampled grid");
    }
}
