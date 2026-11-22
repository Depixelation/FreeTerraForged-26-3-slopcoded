package etcodehome.freeterraforged.world.worldgen.feature.template;

import java.util.function.BiPredicate;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.pathfinder.PathComputationType;

public class BlockUtils {

    public static boolean isSoil(LevelAccessor world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return isSoil(state);
    }

    public static boolean isSoil(BlockState state) {
        return state.is(BlockTags.DIRT)
                || state.is(BlockTags.GRASS_BLOCKS)
                || state.is(BlockTags.SUBSTRATE_OVERWORLD)
                || state.is(BlockTags.SUPPORTS_VEGETATION)
                || state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.DIRT)
                || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.PODZOL)
                || state.is(Blocks.MYCELIUM)
                || state.is(Blocks.ROOTED_DIRT)
                || state.is(Blocks.MUD)
                || state.is(Blocks.MUDDY_MANGROVE_ROOTS)
                || state.is(Blocks.FARMLAND);
    }

    public static boolean isLeavesOrLogs(BlockState state) {
        return state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES);
    }

    public static boolean isVegetation(LevelAccessor world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return state.is(BlockTags.SAPLINGS)
                || state.is(BlockTags.FLOWERS)
                || state.is(Blocks.VINE)
                || state.is(BlockTags.REPLACEABLE_BY_TREES)
                || state.is(BlockTags.REPLACEABLE);
    }

    public static boolean canTreeReplace(LevelAccessor world, BlockPos pos) {
        return TreeFeature.isAirOrLeaves(world, pos) || isVegetation(world, pos);
    }

    public static boolean isSolid(BlockGetter reader, BlockPos pos) {
        BlockState state = reader.getBlockState(pos);
        return isSolid(state);
    }

    public static boolean isSolid(BlockState state) {
        return state.canOcclude() || !state.isPathfindable(PathComputationType.LAND);
    }

    public static boolean isSoilOrRock(LevelAccessor world, BlockPos pos) {
        BlockState block = world.getBlockState(pos);
        return isSoil(block) || block.is(BlockTags.BASE_STONE_OVERWORLD) || block.is(Blocks.STONE) || block.is(Blocks.DEEPSLATE);
    }

    public static boolean isClearOverhead(LevelAccessor world, BlockPos pos, int height, BiPredicate<LevelAccessor, BlockPos> predicate) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        // world.getMaxHeight ?
        int max = Math.min(world.getMaxY() - 1, pos.getY() + height);
        for (int y = pos.getY(); y < max; y++) {
            mutable.set(pos.getX(), y, pos.getZ());
            if (!predicate.test(world, mutable)) {
                return false;
            }
        }
        return true;
    }
}
