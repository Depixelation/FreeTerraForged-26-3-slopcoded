package etcodehome.freeterraforged.world.worldgen.densityfunction;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.densityfunction.DensitySampler;
import net.minecraft.world.level.levelgen.densityfunction.DfRewriteRule;

public record ClampToNearestUnit(DensityFunction function, int resolution) implements MarkerFunction {
	public static final MapCodec<ClampToNearestUnit> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
		DensityFunction.CODEC.fieldOf("function").forGetter(ClampToNearestUnit::function),
		Codec.intRange(1, Integer.MAX_VALUE).fieldOf("resolution").forGetter(ClampToNearestUnit::resolution)
	).apply(instance, ClampToNearestUnit::new));

	public ClampToNearestUnit {
		if (resolution <= 0) {
			throw new IllegalArgumentException("Density quantization resolution must be positive");
		}
	}
	
	@Override
	public double compute(FunctionContext ctx) {
		return this.computeClamped(MarkerFunction.compute(this.function, ctx));
	}

	@Override
	public DensitySampler compileSampler(CompileContext context) {
		DensitySampler inner = this.function.compileSampler(context);
		return MarkerFunction.pointSampler((samplerCtx, x, y, z) -> (float) this.computeClamped(inner.sampleValue(samplerCtx, x, y, z)));
	}

	@Override
	public DensityFunction rewriteChildren(DfRewriteRule rule) {
		return new ClampToNearestUnit(rule.rewrite(this.function), this.resolution);
	}

	@Override
	public int domainAxes() {
		return this.function.domainAxes();
	}

	@Override
	public DensityFunction mapAll(Visitor visitor) {
		return visitor.apply(new ClampToNearestUnit(MarkerFunction.mapAll(this.function, visitor), this.resolution));
	}

	@Override
	public double minValue() {
		return this.computeClamped(this.function.range().min());
	}

	@Override
	public double maxValue() {
		return this.computeClamped(this.function.range().max());
	}

	@Override
	public MapCodec<ClampToNearestUnit> codec() {
		return CODEC;
	}
	
	private double computeClamped(double value) {
		double scaled = value * this.resolution;
		if (!Double.isFinite(scaled)) {
			// Preserve infinite bounds. For finite values whose product overflows, one
			// quantization unit is already far smaller than their representable spacing.
			return value;
		}
		double truncated = scaled < 0.0D ? Math.ceil(scaled) : Math.floor(scaled);
		return (truncated + 1.0D) / this.resolution;
	}
}
