package etcodehome.freeterraforged.client.debug;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import net.minecraft.client.gui.components.debug.DebugScreenDisplayer;
import net.minecraft.resources.Identifier;

import etcodehome.freeterraforged.world.worldgen.cell.Cell;
import etcodehome.freeterraforged.world.worldgen.cell.rivermap.ContinentalHydrology;
import etcodehome.freeterraforged.world.worldgen.cell.rivermap.river.RiverCarverSettings;
import etcodehome.freeterraforged.world.worldgen.cell.terrain.TerrainCategory;
import etcodehome.freeterraforged.world.worldgen.cell.terrain.TerrainType;

public class DebugEntryFreeTerraForgedTest {

    @Test
    void testIdentifier() {
        assertEquals("freeterraforged", DebugEntryFreeTerraForged.ID.getNamespace());
        assertEquals("terrain_debug", DebugEntryFreeTerraForged.ID.getPath());
    }

    @Test
    void testStepIdFormatting() {
        assertEquals("Base", formatStep(-1));
        assertEquals("Ramp", formatStep(-2));
        assertEquals("Step 0", formatStep(0));
        assertEquals("Step 1", formatStep(1));
    }

    private static String formatStep(int stepId) {
        return stepId == -2 ? "Ramp" : (stepId == -1 ? "Base" : "Step " + stepId);
    }

    @Test
    void testFormattingOutputFormat() {
        Cell cell = new Cell();
        cell.terrain = TerrainType.FLATS;
        cell.height = 0.25f;
        cell.gradient = 0.05f;
        cell.continentEdge = 0.6f;
        cell.waterTable = 0.45f;
        cell.riverMask = 0.8f;
        cell.riverZone = RiverCarverSettings.RiverZone.Riverbed;
        cell.temperature = 0.5f;
        cell.moisture = 0.6f;
        cell.erosion = 0.1f;
        cell.weirdness = -0.2f;
        cell.sediment = 0.03f;
        cell.heightErosion = 0.01f;

        String presetName = "FreeTerraForged";
        String cacheStatus = "Filtered Cache";
        String line1 = String.format(Locale.ROOT, "[FreeTerraForged] Preset: %s (%s)", presetName, cacheStatus);
        assertEquals("[FreeTerraForged] Preset: FreeTerraForged (Filtered Cache)", line1);

        int scaledY = 64;
        String line2 = String.format(
            Locale.ROOT,
            "FTF Terrain: %s (%s) | Height: %.4f (Y=%d) | Steepness: %.4f",
            cell.terrain.getName(),
            cell.terrain.getCategory().name(),
            cell.height,
            scaledY,
            cell.gradient
        );
        assertEquals("FTF Terrain: flats (FLATLAND) | Height: 0.2500 (Y=64) | Steepness: 0.0500", line2);

        int stepId = ContinentalHydrology.getStepId(cell.waterTable);
        String stepStr = formatStep(stepId);
        String line3 = String.format(
            Locale.ROOT,
            "FTF Hydrology: ContEdge: %.4f | WaterTable: %.4f (%s) | RiverMask: %.4f (%s)",
            cell.continentEdge,
            cell.waterTable,
            stepStr,
            cell.riverMask,
            cell.riverZone.name()
        );
        assertTrue(line3.startsWith("FTF Hydrology: ContEdge: 0.6000 | WaterTable: 0.4500 ("));
        assertTrue(line3.contains("RiverMask: 0.8000 (Riverbed)"));

        String line4 = String.format(
            Locale.ROOT,
            "FTF Climate: T: %.4f | M: %.4f | E: %.4f | W: %.4f | Sed: %.4f | HE: %.4f",
            cell.temperature,
            cell.moisture,
            cell.erosion,
            cell.weirdness,
            cell.sediment,
            cell.heightErosion
        );
        assertEquals("FTF Climate: T: 0.5000 | M: 0.6000 | E: 0.1000 | W: -0.2000 | Sed: 0.0300 | HE: 0.0100", line4);
    }
}
