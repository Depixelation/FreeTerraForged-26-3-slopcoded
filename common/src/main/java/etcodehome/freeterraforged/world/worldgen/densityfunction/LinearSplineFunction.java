package etcodehome.freeterraforged.world.worldgen.densityfunction;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import etcodehome.freeterraforged.world.worldgen.noise.NoiseUtil;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunctions;
import net.minecraft.world.level.levelgen.densityfunction.DensitySampler;
import net.minecraft.world.level.levelgen.densityfunction.DfRewriteRule;

public record LinearSplineFunction(DensityFunction input, List<Pair<Double, DensityFunction>> points, double minValue, double maxValue) implements MarkerFunction {
	public static final MapCodec<LinearSplineFunction> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
		DensityFunction.CODEC.fieldOf("input").forGetter(LinearSplineFunction::input),
		ExtraCodecs.nonEmptyList(Codec.pair(Codec.DOUBLE, DensityFunction.CODEC).listOf()).fieldOf("points").forGetter(LinearSplineFunction::points)
	).apply(instance, LinearSplineFunction::new));
	
	public LinearSplineFunction(DensityFunction input, List<Pair<Double, DensityFunction>> points) {
		this(input, points, min(points), max(points));
	}
	
	@Override
	public double compute(FunctionContext ctx) {
		double in = MarkerFunction.compute(this.input, ctx);
		int pointCount = this.points.size();
		
		Pair<Double, DensityFunction> first = this.points.get(0);
		if(in <= first.getFirst()) {
			return MarkerFunction.compute(first.getSecond(), ctx);
		}

		Pair<Double, DensityFunction> last = this.points.get(pointCount - 1);
		if(in >= last.getFirst()) {
			return MarkerFunction.compute(last.getSecond(), ctx);
		}
		
		int index = Mth.binarySearch(0, pointCount, i -> in < this.points.get(i).getFirst()) - 1;
		Pair<Double, DensityFunction> start = this.points.get(index);
		Pair<Double, DensityFunction> end = this.points.get(index + 1);
		double min = start.getFirst();
		double max = end.getFirst();
		double from = MarkerFunction.compute(start.getSecond(), ctx);
		double to = MarkerFunction.compute(end.getSecond(), ctx);
		
		double lerp = NoiseUtil.map(in, 0.0D, 1.0D, min, max);
		lerp = NoiseUtil.clamp(lerp, 0.0D, 1.0D);
		return NoiseUtil.lerp(from, to, lerp);
	}

	@Override
	public DensitySampler compileSampler(CompileContext context) {
		DensitySampler inputSampler = this.input.compileSampler(context);
		int pointCount = this.points.size();
		double[] keys = new double[pointCount];
		DensitySampler[] samplers = new DensitySampler[pointCount];
		for (int i = 0; i < pointCount; i++) {
			Pair<Double, DensityFunction> p = this.points.get(i);
			keys[i] = p.getFirst();
			samplers[i] = p.getSecond().compileSampler(context);
		}
		return MarkerFunction.pointSampler((samplerCtx, x, y, z) -> {
			double in = inputSampler.sampleValue(samplerCtx, x, y, z);
			if (in <= keys[0]) {
				return samplers[0].sampleValue(samplerCtx, x, y, z);
			}
			if (in >= keys[pointCount - 1]) {
				return samplers[pointCount - 1].sampleValue(samplerCtx, x, y, z);
			}
			int index = Mth.binarySearch(0, pointCount, i -> in < keys[i]) - 1;
			double min = keys[index];
			double max = keys[index + 1];
			double from = samplers[index].sampleValue(samplerCtx, x, y, z);
			double to = samplers[index + 1].sampleValue(samplerCtx, x, y, z);
			double lerp = NoiseUtil.clamp(NoiseUtil.map(in, 0.0D, 1.0D, min, max), 0.0D, 1.0D);
			return (float) NoiseUtil.lerp(from, to, lerp);
		});
	}

	@Override
	public DensityFunction rewriteChildren(DfRewriteRule rule) {
		return new LinearSplineFunction(
			rule.rewrite(this.input),
			this.points.stream().map(point -> Pair.of(point.getFirst(), rule.rewrite(point.getSecond()))).toList()
		);
	}

	@Override
	public int domainAxes() {
		int axes = this.input.domainAxes();
		for (Pair<Double, DensityFunction> p : this.points) {
			axes |= p.getSecond().domainAxes();
		}
		return axes;
	}

	@Override
	public DensityFunction mapAll(Visitor visitor) {
		return visitor.apply(new LinearSplineFunction(MarkerFunction.mapAll(this.input, visitor), this.points.stream().map((point) -> {
			return Pair.of(point.getFirst(), MarkerFunction.mapAll(point.getSecond(), visitor));
		}).toList()));
	}

	@Override
	public MapCodec<LinearSplineFunction> codec() {
		return CODEC;
	}

	public static LinearSplineFunction.Builder builder(DensityFunction input) {
		return new LinearSplineFunction.Builder(input);
	}
	
	private static float min(List<Pair<Double, DensityFunction>> points) {
		return (float) points.stream().map(Pair::getSecond).mapToDouble(fn -> fn.range().min()).min().orElseThrow();
	}
	
	private static float max(List<Pair<Double, DensityFunction>> points) {
		return (float) points.stream().map(Pair::getSecond).mapToDouble(fn -> fn.range().max()).max().orElseThrow();
	}
	
	public static class Builder {
		private DensityFunction input;
		private List<Pair<Double, DensityFunction>> points;
		
		public Builder(DensityFunction input) {
			this.input = input;
			this.points = new ArrayList<>();
		}
		
		public Builder addPoint(double point, double value) {
			return this.addPoint(point, DensityFunctions.constant((float) value));
		}

		public Builder addPoint(double point, DensityFunction value) {
			this.points.add(Pair.of(point, value));
			return this;
		}
		
		public LinearSplineFunction build() {
			return new LinearSplineFunction(this.input, ImmutableList.copyOf(this.points));
		}
	}
}
