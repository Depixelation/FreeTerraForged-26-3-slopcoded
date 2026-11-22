package etcodehome.freeterraforged.world.worldgen.densityfunction;

import java.util.Arrays;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.Interval;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.densityfunction.DensitySampler;
import net.minecraft.world.level.levelgen.densityfunction.DfRewriteRule;

public record ConditionalFlatCache(DensityFunction function) implements MarkerFunction {
	public static final MapCodec<ConditionalFlatCache> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
		DensityFunction.CODEC.fieldOf("function").forGetter(ConditionalFlatCache::function)
	).apply(instance, ConditionalFlatCache::new));

	@Override
	public double compute(FunctionContext ctx) {
		return MarkerFunction.compute(this.function, ctx);
	}
	
	@Override
	public MapCodec<ConditionalFlatCache> codec() {
		return CODEC;
	}

	@Override
	public DensitySampler compileSampler(CompileContext context) {
		return this.function.compileSampler(context);
	}

	@Override
	public DensityFunction rewriteChildren(DfRewriteRule rule) {
		return new ConditionalFlatCache(rule.rewrite(this.function));
	}

	@Override
	public Interval range() {
		return this.function.range();
	}

	@Override
	public int domainAxes() {
		return this.function.domainAxes();
	}

	@Override
	public DensityFunction mapAll(Visitor visitor) {
		return visitor.apply(new ConditionalFlatCache(MarkerFunction.mapAll(this.function, visitor)));
	}
	
	public class Cache implements MarkerFunction.Mapped {
		private int firstBlockX;
		private int firstBlockZ;
		private int width;
		private float[] cache;
		
		public Cache(int firstBlockX, int firstBlockZ, int width) {
			this.firstBlockX = firstBlockX;
			this.firstBlockZ = firstBlockZ;
			this.width = width;
			this.cache = new float[width * width];
			Arrays.fill(this.cache, Float.NaN);
		}
		
		@Override
		public double compute(FunctionContext ctx) {
            int blockX = ctx.blockX();
            int blockZ = ctx.blockZ();
            int cacheX = blockX - this.firstBlockX;
            int cacheZ = blockZ - this.firstBlockZ;
            if (cacheX >= 0 && cacheZ >= 0 && cacheX < this.width && cacheZ < this.width) {
            	int index = cacheX * this.width + cacheZ;
            	if(Float.isNaN(this.cache[index])) {
            		this.cache[index] = (float) MarkerFunction.compute(ConditionalFlatCache.this.function, ctx);
            	}
                return this.cache[index];
            }
            return MarkerFunction.compute(ConditionalFlatCache.this.function, ctx);
		}

		@Override
		public double minValue() {
			return ConditionalFlatCache.this.function.range().min();
		}

		@Override
		public double maxValue() {
			return ConditionalFlatCache.this.function.range().max();
		}		
	}
}
