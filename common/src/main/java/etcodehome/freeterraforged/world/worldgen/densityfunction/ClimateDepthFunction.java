package etcodehome.freeterraforged.world.worldgen.densityfunction;

import java.util.function.Supplier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.densityfunction.DensitySampler;
import etcodehome.freeterraforged.world.worldgen.cell.Cell;
import etcodehome.freeterraforged.world.worldgen.cell.heightmap.WorldLookup;

public record ClimateDepthFunction(
	Supplier<WorldLookup> deferredLookup,
	int worldDepth,
	int worldHeight
) implements MarkerFunction.Mapped {

	private static final class ThreadMru {
		int blockX = Integer.MIN_VALUE;
		int blockZ = Integer.MIN_VALUE;
		int surfaceY;
	}
	private static final ThreadLocal<ThreadMru> THREAD_MRU = ThreadLocal.withInitial(ThreadMru::new);

	public static void clearThreadCaches() {
		ThreadMru mru = THREAD_MRU.get();
		mru.blockX = Integer.MIN_VALUE;
		mru.blockZ = Integer.MIN_VALUE;
	}

	@Override
	public double compute(FunctionContext ctx) {
		return this.computeAt(ctx.blockX(), ctx.blockY(), ctx.blockZ());
	}

	public double computeAt(int bx, int by, int bz) {
		ThreadMru mru = THREAD_MRU.get();
		int surfaceY;
		if (mru.blockX == bx && mru.blockZ == bz) {
			surfaceY = mru.surfaceY;
		} else {
			WorldLookup lookup = this.deferredLookup.get();
			if (lookup == null || lookup.getContext() == null || lookup.getContext().levels == null) {
				return 0.0D;
			}
			Cell cell = CellSampler.getCachedOrLookup(lookup, bx, bz, false);
			surfaceY = lookup.getContext().levels.scale(cell.height);
			mru.blockX = bx;
			mru.blockZ = bz;
			mru.surfaceY = surfaceY;
		}

		if (by <= surfaceY) {
			int depthSpan = surfaceY + this.worldDepth;
			if (depthSpan <= 0) {
				return 1.5D;
			}
			double factor = (double) (surfaceY - by) / (double) depthSpan;
			return Math.clamp(factor, 0.0D, 1.0D) * 1.5D;
		} else {
			int heightSpan = this.worldHeight - surfaceY;
			if (heightSpan <= 0) {
				return -1.5D;
			}
			double factor = (double) (by - surfaceY) / (double) heightSpan;
			return -Math.clamp(factor, 0.0D, 1.0D) * 1.5D;
		}
	}

	@Override
	public DensitySampler compileSampler(CompileContext context) {
		return MarkerFunction.pointSampler((samplerCtx, x, y, z) -> (float) this.computeAt(x, y, z));
	}

	@Override
	public DensityFunction mapAll(Visitor visitor) {
		return visitor.apply(this);
	}

	@Override
	public double minValue() {
		return -1.5D;
	}

	@Override
	public double maxValue() {
		return 1.5D;
	}


	public record Marker(int worldDepth, int worldHeight) implements MarkerFunction {
		public static final MapCodec<Marker> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			Codec.INT.fieldOf("world_depth").forGetter(Marker::worldDepth),
			Codec.INT.fieldOf("world_height").forGetter(Marker::worldHeight)
		).apply(instance, Marker::new));

		@Override
		public MapCodec<Marker> codec() {
			return CODEC;
		}
	}
}
