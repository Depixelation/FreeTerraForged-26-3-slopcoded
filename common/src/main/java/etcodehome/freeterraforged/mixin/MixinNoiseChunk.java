package etcodehome.freeterraforged.mixin;

import java.util.List;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.Beardifier;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.densityfunction.DensityVolume;
import etcodehome.freeterraforged.data.worldgen.preset.settings.Preset;
import etcodehome.freeterraforged.data.worldgen.preset.settings.WorldSettings;
import etcodehome.freeterraforged.world.worldgen.GeneratorContext;
import etcodehome.freeterraforged.world.worldgen.FTFRandomState;
import etcodehome.freeterraforged.world.worldgen.IChunkCellsHolder;
import etcodehome.freeterraforged.world.worldgen.cell.Cell;
import etcodehome.freeterraforged.world.worldgen.densityfunction.CellSampler;
import etcodehome.freeterraforged.world.worldgen.perf.WorldgenPerfTracker;

@Mixin(NoiseChunk.class)
public class MixinNoiseChunk implements IChunkCellsHolder {

	@Shadow
	@Final
	private RandomState randomState;

	@Unique
	private int chunkX, chunkZ;
	@Unique
	@Nullable
	private Aquifer.FluidPicker freeterraforged$fluidPicker;
	@Unique
	@Nullable
	private Cell[] freeterraforged$chunkCells;

	@Inject(
		method = "<init>",
		at = @At(
			value = "INVOKE",
			target = "Ljava/lang/Object;<init>()V",
			shift = At.Shift.AFTER
		)
	)
	private void freeterraforged$initializeNoiseChunk(
		RandomState randomState,
		Beardifier beardifier,
		NoiseGeneratorSettings generatorSettings,
		Aquifer.FluidPicker fluidPicker,
		Blender blender,
		DensityVolume volume,
		CallbackInfo callback
	) {
		this.chunkX = SectionPos.blockToSectionCoord(volume.minBlockX());
		this.chunkZ = SectionPos.blockToSectionCoord(volume.minBlockZ());
		if ((Object) randomState instanceof FTFRandomState rtfRandomState) {
			Preset preset = rtfRandomState.preset();
			GeneratorContext generatorContext = rtfRandomState.generatorContext();
			if (preset != null && generatorContext != null) {
				this.freeterraforged$fluidPicker = this.freeterraforged$createFluidPicker(
					preset, generatorContext, generatorSettings
				);
				ChunkAccess activeChunk = IChunkCellsHolder.ACTIVE_CHUNK.get();
				if (activeChunk instanceof IChunkCellsHolder holder
						&& activeChunk.getPos().x() == this.chunkX
						&& activeChunk.getPos().z() == this.chunkZ) {
					this.freeterraforged$chunkCells = holder.freeterraforged$getOrCreateChunkCells(generatorContext);
				} else {
					this.freeterraforged$chunkCells = generatorContext.filteredChunkCache.getFilteredChunk(this.chunkX, this.chunkZ);
				}
			}
		}
	}

	@ModifyArg(
		method = "<init>",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/levelgen/Aquifer;createDisabled(Lnet/minecraft/world/level/levelgen/Aquifer$FluidPicker;)Lnet/minecraft/world/level/levelgen/Aquifer;"
		),
		index = 0
	)
	private Aquifer.FluidPicker freeterraforged$modifyDisabledFluidPicker(Aquifer.FluidPicker fluidPicker) {
		return this.freeterraforged$fluidPicker == null ? fluidPicker : this.freeterraforged$fluidPicker;
	}

	@ModifyArg(
		method = "<init>",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/levelgen/Aquifer$Config;create(Lnet/minecraft/world/level/levelgen/densityfunction/DensitySamplerSet;Lnet/minecraft/world/level/levelgen/PositionalRandomFactory;Lnet/minecraft/world/level/levelgen/densityfunction/DensityVolume;Lnet/minecraft/world/level/levelgen/Aquifer$FluidPicker;)Lnet/minecraft/world/level/levelgen/Aquifer;"
		),
		index = 3
	)
	private Aquifer.FluidPicker freeterraforged$modifyFluidPicker(Aquifer.FluidPicker fluidPicker) {
		return this.freeterraforged$fluidPicker == null ? fluidPicker : this.freeterraforged$fluidPicker;
	}

	@Unique
	private Aquifer.FluidPicker freeterraforged$createFluidPicker(
		Preset preset,
		GeneratorContext generatorContext,
		NoiseGeneratorSettings noiseGeneratorSettings
	) {
		int globalLavaLevel = preset.world().properties.lavaLevel;
		int seaLevel = noiseGeneratorSettings.seaLevel();
		int oceanDepth = preset.world().properties.oceanDepth;
		Aquifer.FluidStatus lava = new Aquifer.FluidStatus(globalLavaLevel, Blocks.LAVA.defaultBlockState());
		Aquifer.FluidStatus defaultFluid = new Aquifer.FluidStatus(seaLevel, noiseGeneratorSettings.defaultFluid());
		int oceanLavaLevel = seaLevel - oceanDepth - 5;
		if (globalLavaLevel <= oceanLavaLevel) {
			return (x, y, z) -> y < Math.min(globalLavaLevel, seaLevel) ? lava : defaultFluid;
		}

		WorldSettings.ControlPoints controlPoints = preset.world().controlPoints;
		float shallowOceanCP = controlPoints.shallowOcean;
		float coastCP = controlPoints.coast;
		float transitionRange = coastCP - shallowOceanCP;
		Aquifer.FluidStatus oceanLava = new Aquifer.FluidStatus(oceanLavaLevel, Blocks.LAVA.defaultBlockState());
		int[] columnCache = { Integer.MIN_VALUE, Integer.MIN_VALUE, globalLavaLevel };
		Aquifer.FluidStatus[] cachedLavaStatus = { lava };

		return (x, y, z) -> {
			int effectiveLava;
			Aquifer.FluidStatus effectiveLavaStatus;
			if (x == columnCache[0] && z == columnCache[1]) {
				effectiveLava = columnCache[2];
				effectiveLavaStatus = cachedLavaStatus[0];
			} else {
				effectiveLava = globalLavaLevel;
				effectiveLavaStatus = lava;
				boolean perfActive = WorldgenPerfTracker.isActive();
				WorldgenPerfTracker.ThreadStats ts = perfActive ? WorldgenPerfTracker.threadStats() : null;
				WorldgenPerfTracker.ApplyCellCaller prevCaller = null;
				if (ts != null) {
					prevCaller = ts.currentCaller;
					ts.currentCaller = WorldgenPerfTracker.ApplyCellCaller.AQUIFER;
				}
				float continentEdge;
				try {
					continentEdge = CellSampler.getCachedOrLookup(generatorContext.lookup, x, z, false).continentEdge;
				} finally {
					if (ts != null) {
						ts.currentCaller = prevCaller;
					}
				}

				if (continentEdge <= shallowOceanCP) {
					effectiveLava = oceanLavaLevel;
					effectiveLavaStatus = oceanLava;
				} else if (continentEdge < coastCP && transitionRange > 0) {
					float t = (continentEdge - shallowOceanCP) / transitionRange;
					effectiveLava = (int) Mth.lerp(t, oceanLavaLevel, globalLavaLevel);
					effectiveLavaStatus = new Aquifer.FluidStatus(effectiveLava, Blocks.LAVA.defaultBlockState());
				}
				columnCache[0] = x;
				columnCache[1] = z;
				columnCache[2] = effectiveLava;
				cachedLavaStatus[0] = effectiveLavaStatus;
			}

			return y < Math.min(effectiveLava, seaLevel) ? effectiveLavaStatus : defaultFluid;
		};
	}

	@Override
	@Nullable
	public Cell[] freeterraforged$getChunkCells() {
		return this.freeterraforged$chunkCells;
	}

	@Override
	public void freeterraforged$setChunkCells(Cell[] cells) {
		this.freeterraforged$chunkCells = cells;
	}

	@Override
	public Cell[] freeterraforged$getOrCreateChunkCells(GeneratorContext context) {
		if (this.freeterraforged$chunkCells == null) {
			this.freeterraforged$chunkCells = context.filteredChunkCache.getFilteredChunk(this.chunkX, this.chunkZ);
		}
		return this.freeterraforged$chunkCells;
	}

	@Override
	public Cell freeterraforged$getCell(int localX, int localZ, GeneratorContext context) {
		Cell[] cells = this.freeterraforged$getOrCreateChunkCells(context);
		return cells[(localZ & 15) * 16 + (localX & 15)];
	}
}
