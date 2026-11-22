package etcodehome.freeterraforged.world.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import etcodehome.freeterraforged.world.worldgen.noise.module.Noise;
import etcodehome.freeterraforged.world.worldgen.noise.module.Noises;

public record DiskFeature(
	Holder<BlockStateProvider> stateProvider,
	BlockPredicate target,
	IntProvider radius,
	int halfHeight
) implements Feature {
	public static final MapCodec<DiskFeature> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
		BlockStateProvider.CODEC.fieldOf("state_provider").forGetter(DiskFeature::stateProvider),
		BlockPredicate.CODEC.fieldOf("target").forGetter(DiskFeature::target),
		IntProviders.codec(0, 8).fieldOf("radius").forGetter(DiskFeature::radius),
		Codec.intRange(0, 4).fieldOf("half_height").forGetter(DiskFeature::halfHeight)
	).apply(instance, DiskFeature::new));

    private static final Noise DOMAIN = Noises.simplex(1, 6, 3);

	@Override
	public MapCodec<DiskFeature> codec() {
		return CODEC;
	}

	@Override
	public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos pos) {
        if (!level.getFluidState(pos).is(FluidTags.WATER)) {
            return false;
        } else {
            int cRadius = 6;
            int ySize = 5;

            int i = 0;
            int rad = 4 + random.nextInt(cRadius);
            float radius2 = (rad * rad) * 0.65F;
            BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();

            for(int x = pos.getX() - rad; x <= pos.getX() + rad; ++x) {
                for(int z = pos.getZ() - rad; z <= pos.getZ() + rad; ++z) {
                    int dx = x - pos.getX();
                    int dz = z - pos.getZ();
                    float rad2 = DOMAIN.compute(x, z, 0) * radius2;
                    if (dx * dx + dz * dz <= rad2) {
                        for(int y = pos.getY() - ySize; y <= pos.getY() + ySize && y + 1 < generator.getGenDepth(); ++y) {
                            blockPos.set(x, y, z);
                            
                            if(this.target.test(level, blockPos)) {
                                level.setBlock(blockPos, this.stateProvider.value().getState(level, random, blockPos), 2);
                                ++i;
                            }
                        }
                    }
                }
            }
            return i > 0;
        }
	}
}
