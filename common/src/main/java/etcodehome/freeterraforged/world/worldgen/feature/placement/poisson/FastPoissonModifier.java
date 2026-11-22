package etcodehome.freeterraforged.world.worldgen.feature.placement.poisson;

import java.util.Random;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import etcodehome.freeterraforged.world.worldgen.FTFRandomState;
import etcodehome.freeterraforged.world.worldgen.GeneratorContext;
import etcodehome.freeterraforged.world.worldgen.cell.heightmap.WorldLookup;
import etcodehome.freeterraforged.world.worldgen.feature.placement.FTFPlacementModifiers;
import etcodehome.freeterraforged.world.worldgen.noise.module.Noise;
import etcodehome.freeterraforged.world.worldgen.noise.module.Noises;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;

public class FastPoissonModifier implements PlacementModifier {
	public static final MapCodec<FastPoissonModifier> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
		Codec.INT.fieldOf("radius").forGetter((p) -> p.radius),
		Codec.FLOAT.fieldOf("scale").forGetter((p) -> p.scale),
		Codec.FLOAT.fieldOf("jitter").forGetter((p) -> p.jitter),
		Codec.FLOAT.fieldOf("biome_fade").forGetter((p) -> p.biomeFade),
		Codec.INT.fieldOf("density_variance_scale").forGetter((p) -> p.densityVariationScale),
		Codec.FLOAT.fieldOf("density_variation").forGetter((p) -> p.densityVariation)
	).apply(instance, FastPoissonModifier::new));
	
	protected int radius;
	protected float scale;
	protected float jitter;
	protected float biomeFade;
	protected int densityVariationScale;
	protected float densityVariation;
	
	public FastPoissonModifier(
		int radius,
		float scale,
		float jitter,
		float biomeFade,
		int densityVariationScale,
		float densityVariation
	) {
		this.radius = radius;
		this.scale = scale;
		this.jitter = jitter;
		this.biomeFade = biomeFade;
		this.densityVariationScale = densityVariationScale;
		this.densityVariation = densityVariation;
	}
	
	public Stream<BlockPos> getPositions(PlacementContext ctx, RandomSource random, BlockPos pos) {
		Stream.Builder<BlockPos> builder = Stream.builder();
		this.modify(ctx, random, pos, builder::accept);
		return builder.build();
	}

	@Override
	public void modify(PlacementContext ctx, RandomSource random, BlockPos pos, Consumer<BlockPos> output) {
		WorldGenLevel level = ctx.getLevel();
		ChunkAccess chunk = ctx.getLevel().getChunk(pos);
		long levelSeed = level.getSeed();
		int seed = (int) levelSeed + 234523;
		ChunkPos chunkPos = chunk.getPos();
		int chunkX = chunkPos.x();
		int chunkZ = chunkPos.z();
		RandomState randomState = level.getLevel().getChunkSource().randomState();
		if (this.biomeFade > BiomeVariance.MIN_FADE
			&& (Object) randomState instanceof FTFRandomState rtfRandomState) {
			GeneratorContext generatorContext = rtfRandomState.generatorContext();
			if (generatorContext != null) {
				this.visitPositions(
					seed,
					levelSeed,
					chunkX,
					chunkZ,
					generatorContext.lookup,
					output
				);
				return;
			}
		}
		this.visitPositions(seed, levelSeed, chunkX, chunkZ, null, output);
	}

	@Override
	public MapCodec<FastPoissonModifier> codec() {
		return FTFPlacementModifiers.FAST_POISSON;
	}

	private void visitPositions(int seed, long levelSeed, int chunkX, int chunkZ, WorldLookup lookup, Consumer<BlockPos> output) {
		FastPoisson poisson = FastPoisson.LOCAL_POISSON.get();
		DensityNoise density = this.getDensityNoise(seed, lookup);
		FastPoissonContext poissonConfig = new FastPoissonContext(this.radius, this.jitter, this.scale, density);
		poisson.visit(seed, chunkX, chunkZ, new Random(levelSeed), poissonConfig, output, (x, z, b) -> {
			b.accept(new BlockPos(x, 0, z));
		});
	}

	private DensityNoise getDensityNoise(int seed, WorldLookup lookup) {
		BiomeVariance biomeVariance = lookup == null
			? BiomeVariance.NONE
			: new BiomeVariance(lookup, this.biomeFade);

		Noise densityVariance = Noises.one();
		if (this.densityVariation > 0) {
			densityVariance = Noises.simplex(seed + 1, this.densityVariationScale, 1);
			densityVariance = Noises.mul(densityVariance, this.densityVariation);
			densityVariance = Noises.add(densityVariance, 1.0F - this.densityVariation);
		}

		return new DensityNoise(biomeVariance, densityVariance);
	}
}
