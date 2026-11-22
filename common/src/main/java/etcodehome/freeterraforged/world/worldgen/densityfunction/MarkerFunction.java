package etcodehome.freeterraforged.world.worldgen.densityfunction;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.Interval;
import net.minecraft.world.level.levelgen.densityfunction.DensityBuffer;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.densityfunction.DensitySampler;
import net.minecraft.world.level.levelgen.densityfunction.DensityVolume;
import net.minecraft.world.level.levelgen.densityfunction.DfRewriteRule;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;

public interface MarkerFunction extends DensityFunction, DensitySampler {

	@FunctionalInterface
	interface PointSampler extends DensitySampler {
		@Override
		default void sampleVolume(SamplerContext ctx, DensityBuffer buffer, DensityVolume volume) {
			DensitySampler.sampleVolumeNaive(ctx, buffer, volume, this);
		}
	}

	static DensitySampler pointSampler(PointSampler sampler) {
		return sampler;
	}

	static DensitySampler.Bound constantBound(float value) {
		return pointSampler((ctx, x, y, z) -> value).bind(SamplerContext.EMPTY_UNCACHED);
	}

	interface FunctionContext {
		int blockX();
		int blockY();
		int blockZ();
	}

	record SinglePointContext(int blockX, int blockY, int blockZ) implements FunctionContext {
	}

	final class MutableFunctionContext implements FunctionContext {
		private int blockX;
		private int blockY;
		private int blockZ;

		public MutableFunctionContext at(int x, int y, int z) {
			this.blockX = x;
			this.blockY = y;
			this.blockZ = z;
			return this;
		}

		@Override
		public int blockX() {
			return this.blockX;
		}

		@Override
		public int blockY() {
			return this.blockY;
		}

		@Override
		public int blockZ() {
			return this.blockZ;
		}
	}

	ThreadLocal<MutableFunctionContext> CONTEXT = ThreadLocal.withInitial(MutableFunctionContext::new);

	@FunctionalInterface
	interface Visitor extends DfRewriteRule {
		DensityFunction apply(DensityFunction function);

		@Override
		default DensityFunction rewrite(DensityFunction function) {
			return this.apply(function.rewriteChildren(this));
		}
	}

	static double compute(DensityFunction function, FunctionContext ctx) {
		if (function instanceof MarkerFunction marker) {
			return marker.compute(ctx);
		}
		DensitySampler sampler = function.compileSampler(new CompileContext() {
			@Override
			public net.minecraft.world.level.levelgen.synth.Noise createNoiseSampler(net.minecraft.core.Holder<net.minecraft.world.level.levelgen.synth.NormalNoise> holder) {
				return holder.value().create(net.minecraft.util.RandomSource.create(0L));
			}

			@Override
			public net.minecraft.util.RandomSource createRandom(net.minecraft.resources.Identifier id) {
				return net.minecraft.util.RandomSource.create(0L);
			}

			@Override
			public net.minecraft.util.RandomSource createEndIslandRandom() {
				return net.minecraft.util.RandomSource.create(0L);
			}
		});
		return sampler.sampleValue(SamplerContext.EMPTY_UNCACHED, ctx.blockX(), ctx.blockY(), ctx.blockZ());
	}

	static DensityFunction mapAll(DensityFunction function, Visitor visitor) {
		return visitor.rewrite(function);
	}

	default double compute(FunctionContext ctx) {
		return 0.0;
	}

	default double minValue() {
		return Float.NEGATIVE_INFINITY;
	}

	default double maxValue() {
		return Float.POSITIVE_INFINITY;
	}

	default DensityFunction mapAll(Visitor visitor) {
		return visitor.rewrite(this);
	}

	@Override
	default void sampleVolume(SamplerContext ctx, DensityBuffer buffer, DensityVolume volume) {
		DensitySampler.sampleVolumeNaive(ctx, buffer, volume, this);
	}

	@Override
	default float sampleValue(SamplerContext ctx, int x, int y, int z) {
		return (float) this.compute(CONTEXT.get().at(x, y, z));
	}

	@Override
	default DensitySampler compileSampler(CompileContext context) {
		return this;
	}

	default DensitySampler.Bound bind() {
		return this.bind(SamplerContext.EMPTY_UNCACHED);
	}

	@Override
	default DensityFunction rewriteChildren(DfRewriteRule rule) {
		return this;
	}

	@Override
	default Interval range() {
		return Interval.of((float) this.minValue(), (float) this.maxValue());
	}

	@Override
	default int domainAxes() {
		return DensityFunction.ALL_AXES;
	}

	public interface Mapped extends MarkerFunction {

		@Override
		default MapCodec<? extends DensityFunction> codec() {
			throw new UnsupportedOperationException();
		}
	}
}
