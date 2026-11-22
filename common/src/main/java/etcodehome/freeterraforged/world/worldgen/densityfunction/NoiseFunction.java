package etcodehome.freeterraforged.world.worldgen.densityfunction;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.densityfunction.DensitySampler;
import net.minecraft.world.level.levelgen.densityfunction.DfRewriteRule;
import etcodehome.freeterraforged.world.worldgen.noise.module.Noise;

public record NoiseFunction(Holder<Noise> noise, int seed) implements MarkerFunction.Mapped {

	@Override
	public double compute(FunctionContext ctx) {
		return this.noise.value().compute(ctx.blockX(), ctx.blockZ(), this.seed);
	}

	@Override
	public DensitySampler compileSampler(CompileContext context) {
		Noise noiseModule = this.noise.value();
		int s = this.seed;
		return MarkerFunction.pointSampler((samplerCtx, x, y, z) -> noiseModule.compute(x, z, s));
	}

	@Override
	public int domainAxes() {
		return DensityFunction.AXIS_X | DensityFunction.AXIS_Z;
	}

	@Override
	public double minValue() {
		return this.noise.value().minValue();
	}

	@Override
	public double maxValue() {
		return this.noise.value().maxValue();
	}
	
	public record Marker(Holder<Noise> noise) implements MarkerFunction {
		public static final MapCodec<Marker> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			Noise.CODEC.fieldOf("noise").forGetter(NoiseFunction.Marker::noise)
		).apply(instance, NoiseFunction.Marker::new));

		@Override
		public MapCodec<NoiseFunction.Marker> codec() {
			return CODEC;
		}

		@Override
		public int domainAxes() {
			return DensityFunction.AXIS_X | DensityFunction.AXIS_Z;
		}

		@Override
		public DensityFunction rewriteChildren(DfRewriteRule rule) {
			if (rule instanceof Noise.Visitor noiseVisitor) {
				return new Marker(Holder.direct(this.noise.value().mapAll(noiseVisitor)));
			}
			return this;
		}

		@Override
		public DensityFunction mapAll(Visitor visitor) {
			DensityFunction self = visitor instanceof Noise.Visitor noiseVisitor ?
				new Marker(Holder.direct(this.noise.value().mapAll(noiseVisitor))) : 
				this;
			return visitor.apply(self);
		}
	}
}
