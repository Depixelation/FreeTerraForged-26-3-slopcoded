package etcodehome.freeterraforged.client.debug;

import java.util.List;
import java.util.Locale;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugScreenDisplayer;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.RandomState;

import etcodehome.freeterraforged.client.data.DefaultPresetArchive;
import etcodehome.freeterraforged.data.worldgen.preset.PresetManager;
import etcodehome.freeterraforged.world.worldgen.FTFRandomState;
import etcodehome.freeterraforged.world.worldgen.GeneratorContext;
import etcodehome.freeterraforged.world.worldgen.cell.Cell;
import etcodehome.freeterraforged.world.worldgen.cell.rivermap.ContinentalHydrology;

public class DebugEntryFreeTerraForged implements DebugScreenEntry {
    public static final Identifier ID = Identifier.fromNamespaceAndPath("freeterraforged", "terrain_debug");

    private int lastX = Integer.MIN_VALUE;
    private int lastZ = Integer.MIN_VALUE;
    private int lastContextHash = 0;
    private List<String> cachedLines = null;

    @Override
    public void display(DebugScreenDisplayer displayer, Level level, LevelChunk clientChunk, LevelChunk serverChunk) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getSingleplayerServer() == null || mc.level == null) {
            return;
        }
        ServerLevel serverLevel = mc.getSingleplayerServer().getLevel(mc.level.dimension());
        if (serverLevel == null) {
            return;
        }
        RandomState randomState = serverLevel.getChunkSource().randomState();
        if (!((Object) randomState instanceof FTFRandomState ftfRandomState) || !ftfRandomState.isTerraForged()) {
            return;
        }
        GeneratorContext generatorContext = ftfRandomState.generatorContext();
        if (generatorContext == null) {
            return;
        }

        Entity camera = mc.getCameraEntity();
        if (camera == null) {
            return;
        }
        BlockPos pos = camera.blockPosition();
        int blockX = pos.getX();
        int blockZ = pos.getZ();
        int contextHash = System.identityHashCode(generatorContext);

        if (blockX == this.lastX && blockZ == this.lastZ && contextHash == this.lastContextHash && this.cachedLines != null) {
            displayer.addToGroup(ID, this.cachedLines);
            return;
        }

        Cell cell = new Cell();
        boolean isFiltered = false;
        if (generatorContext.filteredChunkCache != null) {
            Cell cached = generatorContext.filteredChunkCache.getExistingFilteredCell(blockX, blockZ);
            if (cached != null) {
                cell.copyFrom(cached);
                isFiltered = true;
            }
        }
        if (!isFiltered) {
            generatorContext.lookup.applyCell(cell, blockX, blockZ, true);
        }
        if (cell.gradient == 0.0F) {
            generatorContext.lookup.computeSteepness(cell, blockX, blockZ);
        }

        String presetName;
        if (PresetManager.PM.cachedPreset != null) {
            presetName = PresetManager.PM.cachedPreset.getRawName();
        } else {
            presetName = DefaultPresetArchive.getActiveDefaultPresetName();
        }
        if (presetName == null || presetName.isEmpty()) {
            presetName = "FreeTerraForged";
        }

        String cacheStatus = isFiltered ? "Filtered Cache" : "Raw Lookup";
        String line1 = String.format(Locale.ROOT, "[FreeTerraForged] Preset: %s (%s)", presetName, cacheStatus);

        String terrainName = cell.terrain != null ? cell.terrain.getName() : "None";
        String terrainCategory = (cell.terrain != null && cell.terrain.getCategory() != null)
            ? cell.terrain.getCategory().name()
            : "None";
        int scaledY = generatorContext.levels.scale(cell.height);
        String line2 = String.format(
            Locale.ROOT,
            "FTF Terrain: %s (%s) | Height: %.4f (Y=%d) | Steepness: %.4f",
            terrainName,
            terrainCategory,
            cell.height,
            scaledY,
            cell.gradient
        );

        int stepId = ContinentalHydrology.getStepId(cell.waterTable);
        String stepStr = stepId == -2 ? "Ramp" : (stepId == -1 ? "Base" : "Step " + stepId);
        String riverZoneStr = cell.riverZone != null ? cell.riverZone.name() : "None";
        String line3 = String.format(
            Locale.ROOT,
            "FTF Hydrology: ContEdge: %.4f | WaterTable: %.4f (%s) | RiverMask: %.4f (%s)",
            cell.continentEdge,
            cell.waterTable,
            stepStr,
            cell.riverMask,
            riverZoneStr
        );

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

        List<String> lines = List.of(line1, line2, line3, line4);
        this.lastX = blockX;
        this.lastZ = blockZ;
        this.lastContextHash = contextHash;
        this.cachedLines = lines;

        displayer.addToGroup(ID, lines);
    }
}
