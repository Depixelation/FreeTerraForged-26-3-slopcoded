package etcodehome.freeterraforged.world.worldgen;

import org.jetbrains.annotations.Nullable;
import net.minecraft.world.level.chunk.ChunkAccess;
import etcodehome.freeterraforged.world.worldgen.cell.Cell;

public interface IChunkCellsHolder {
    ThreadLocal<ChunkAccess> ACTIVE_CHUNK = new ThreadLocal<>();
    @Nullable
    Cell[] freeterraforged$getChunkCells();

    void freeterraforged$setChunkCells(Cell[] cells);

    Cell[] freeterraforged$getOrCreateChunkCells(GeneratorContext context);

    Cell freeterraforged$getCell(int localX, int localZ, GeneratorContext context);
}
