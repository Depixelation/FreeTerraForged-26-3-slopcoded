package etcodehome.freeterraforged.mixin;

import java.util.function.Function;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import etcodehome.freeterraforged.data.worldgen.preset.PresetManager;
import etcodehome.freeterraforged.data.worldgen.preset.settings.SpawnType;
import etcodehome.freeterraforged.data.worldgen.preset.settings.WorldSettings;
import etcodehome.freeterraforged.world.worldgen.cell.biome.spawn.SpawnFinderFix;
import etcodehome.freeterraforged.world.worldgen.ChunkFlowField;
import etcodehome.freeterraforged.world.worldgen.FTFRandomState;
import etcodehome.freeterraforged.world.worldgen.GeneratorContext;
import etcodehome.freeterraforged.world.worldgen.IChunkCellsHolder;
import etcodehome.freeterraforged.world.worldgen.IFlowFieldHolder;
import etcodehome.freeterraforged.world.worldgen.biome.BiomeColumnCache;
import etcodehome.freeterraforged.world.worldgen.cell.Cell;
import etcodehome.freeterraforged.world.worldgen.perf.WorldgenPerfTracker;
import etcodehome.freeterraforged.world.worldgen.runtime.TerraForgedChunkGenerator;
@Mixin(NoiseBasedChunkGenerator.class)
abstract class MixinNoiseBasedChunkGenerator extends ChunkGenerator {
	public MixinNoiseBasedChunkGenerator(BiomeSource biomeSource, Function<Holder<Biome>, BiomeGenerationSettings> settingsGetter) {
		super(biomeSource, settingsGetter);
	}

	@Inject(
		method = "getOrigin",
		at = @At("HEAD"),
		cancellable = true
	)
	private void freeterraforged$originFromFtf(RandomState randomState, CallbackInfoReturnable<ChunkPos> cir) {
		if ((Object) this instanceof TerraForgedChunkGenerator) {
			return;
		}
		if ((Object) randomState instanceof FTFRandomState rtfRs && rtfRs.generatorContext() != null) {
			GeneratorContext ctx = rtfRs.generatorContext();
			WorldSettings.Properties properties = ctx.preset != null && ctx.preset.world() != null
				? ctx.preset.world().properties
				: (PresetManager.PM.isLoaded ? PresetManager.PM.worldSettings.properties : null);
			if (properties != null) {
				if (properties.spawnType == SpawnType.WORLD_ORIGIN) {
					cir.setReturnValue(ChunkPos.ZERO);
					return;
				}
				if (properties.spawnType == SpawnType.USER_SELECTED) {
					cir.setReturnValue(new ChunkPos(QuartPos.toBlock(QuartPos.fromBlock(properties.spawnX)) >> 4, QuartPos.toBlock(QuartPos.fromBlock(properties.spawnZ)) >> 4));
					return;
				}
				BlockPos center = properties.spawnType.getSearchCenter(ctx, properties);
				java.util.List<Climate.ParameterPoint> points = properties.spawnType.getParameterPoints();
				if (!points.isEmpty() && properties.spawnType == SpawnType.CONTINENT_CENTER) {
					Climate.Sampler sampler = randomState.createClimateSampler(
						SamplerContext.builder().enableCaches().build()
					);
					BlockPos refined = new SpawnFinderFix(center, points, sampler).result.location();
					cir.setReturnValue(ChunkPos.containing(refined));
					return;
				}
				cir.setReturnValue(new ChunkPos(QuartPos.toBlock(QuartPos.fromBlock(center.getX())) >> 4, QuartPos.toBlock(QuartPos.fromBlock(center.getZ())) >> 4));
			}
		}
	}

	@WrapMethod(method = "doFill")
	private void freeterraforged$wrapDoFill(
		NoiseChunk noiseChunk,
		ChunkAccess chunk,
		Operation<Void> original
	) {
		boolean perfActive = WorldgenPerfTracker.isActive() && ((Object) this instanceof TerraForgedChunkGenerator);
		long t0 = perfActive ? System.nanoTime() : 0L;
		ChunkAccess prev = IChunkCellsHolder.ACTIVE_CHUNK.get();
		IChunkCellsHolder.ACTIVE_CHUNK.set(chunk);
		try {
			original.call(noiseChunk, chunk);
		} finally {
			if (prev != null) {
				IChunkCellsHolder.ACTIVE_CHUNK.set(prev);
			} else {
				IChunkCellsHolder.ACTIVE_CHUNK.remove();
			}
			if (perfActive) {
				WorldgenPerfTracker.recordStage(
					chunk.getPos().x(),
					chunk.getPos().z(),
					WorldgenPerfTracker.Stage.NOISE,
					System.nanoTime() - t0
				);
			}
		}
	}

	@WrapMethod(method = "createNoiseChunk")
	private NoiseChunk freeterraforged$wrapCreateNoiseChunk(
		ChunkAccess chunk,
		StructureManager sm,
		Blender blender,
		RandomState rs,
		NoiseSettings noiseSettings,
		Operation<NoiseChunk> original
	) {
		ChunkAccess prev = IChunkCellsHolder.ACTIVE_CHUNK.get();
		IChunkCellsHolder.ACTIVE_CHUNK.set(chunk);
		try {
			return original.call(chunk, sm, blender, rs, noiseSettings);
		} finally {
			if (prev != null) {
				IChunkCellsHolder.ACTIVE_CHUNK.set(prev);
			} else {
				IChunkCellsHolder.ACTIVE_CHUNK.remove();
			}
		}
	}

	@Inject(
		method = "createNoiseChunk",
		at = @At("RETURN")
	)
	private void freeterraforged$bindChunkCells(ChunkAccess chunk, StructureManager sm, Blender blender, RandomState rs, NoiseSettings noiseSettings, CallbackInfoReturnable<NoiseChunk> cir) {
		if ((Object) rs instanceof FTFRandomState rtfRs && rtfRs.generatorContext() != null) {
			if (cir.getReturnValue() instanceof IChunkCellsHolder noiseChunkHolder && chunk instanceof IChunkCellsHolder chunkHolder) {
				Cell[] cells = noiseChunkHolder.freeterraforged$getChunkCells();
				if (cells != null) {
					chunkHolder.freeterraforged$setChunkCells(cells);
				} else if (chunkHolder.freeterraforged$getChunkCells() != null) {
					noiseChunkHolder.freeterraforged$setChunkCells(chunkHolder.freeterraforged$getChunkCells());
				}
			}
		}
	}

	@Unique
	private void freeterraforged$bakeFlowField(ChunkAccess chunk, GeneratorContext generatorContext) {
		IFlowFieldHolder holder = (IFlowFieldHolder) chunk;
		ChunkFlowField flowField = holder.freeterraforged$getFlowField();
		IChunkCellsHolder cellsHolder = chunk instanceof IChunkCellsHolder ch ? ch : null;
		Cell[] cells = cellsHolder != null ? cellsHolder.freeterraforged$getOrCreateChunkCells(generatorContext) : null;
		for(int z = 0; z < 16; z++) {
			for(int x = 0; x < 16; x++) {
				Cell cell = cells != null
					? cells[z * 16 + x]
					: generatorContext.lookup.lookup(chunk.getPos().getBlockX(x), chunk.getPos().getBlockZ(z));
				if (cell.hasFlow) {
					if (flowField == null) {
						flowField = holder.freeterraforged$getOrCreateFlowField();
					}
					flowField.setFlow(x, z, cell.flowAngle);
				}
			}
		}
	}

	@Redirect(
		method = "decorateBiomeResolver",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/levelgen/blending/Blender;getBiomeResolver(Lnet/minecraft/world/level/biome/BiomeResolver;)Lnet/minecraft/world/level/biome/BiomeResolver;"
		)
	)
	private BiomeResolver freeterraforged$useGeneratorRootBiomeResolver(
		Blender blender,
		BiomeResolver source,
		Blender methodBlender,
		ChunkAccess chunk,
		BiomeResolver methodSource
	) {
		if ((Object) this instanceof TerraForgedChunkGenerator generator) {
			GeneratorContext generatorContext = generator.generatorContext().orElse(null);
			if (generatorContext != null) {
				this.freeterraforged$bakeFlowField(chunk, generatorContext);
				final int chunkQuartX = chunk.getPos().x() << 2;
				final int chunkQuartZ = chunk.getPos().z() << 2;
				final long[] internalRegionX = new long[16];
				final long[] internalRegionZ = new long[16];
				final boolean hasInternalCells;
				if (chunk instanceof IChunkCellsHolder holder) {
					Cell[] chunkCells = holder.freeterraforged$getOrCreateChunkCells(generatorContext);
					if (chunkCells != null) {
						for (int qz = 0; qz < 4; qz++) {
							for (int qx = 0; qx < 4; qx++) {
								Cell cell = chunkCells[(qz * 4) * 16 + (qx * 4)];
								int idx = qz * 4 + qx;
								internalRegionX[idx] = cell.biomeRegionX;
								internalRegionZ[idx] = cell.biomeRegionZ;
							}
						}
						hasInternalCells = true;
					} else {
						hasInternalCells = false;
					}
				} else {
					hasInternalCells = false;
				}

				return blender.getBiomeResolver((quartX, quartY, quartZ) -> {
					int localQx = quartX - chunkQuartX;
					int localQz = quartZ - chunkQuartZ;
					long regionX;
					long regionZ;
					if (hasInternalCells && localQx >= 0 && localQx < 4 && localQz >= 0 && localQz < 4) {
						int idx = localQz * 4 + localQx;
						regionX = internalRegionX[idx];
						regionZ = internalRegionZ[idx];
					} else {
						BiomeColumnCache colCache = BiomeColumnCache.get();
						if (colCache.quartX == quartX && colCache.quartZ == quartZ) {
							regionX = colCache.regionX;
							regionZ = colCache.regionZ;
						} else {
							int blockX = QuartPos.toBlock(quartX);
							int blockZ = QuartPos.toBlock(quartZ);
							generatorContext.lookup.applyBiomeRegion(colCache.scratch.reset(), blockX, blockZ);
							colCache.quartX = quartX;
							colCache.quartZ = quartZ;
							colCache.regionX = colCache.scratch.biomeRegionX;
							colCache.regionZ = colCache.scratch.biomeRegionZ;
							regionX = colCache.regionX;
							regionZ = colCache.regionZ;
						}
					}
					BiomeColumnCache colCache = BiomeColumnCache.get();
					colCache.quartX = quartX;
					colCache.quartZ = quartZ;
					colCache.regionX = regionX;
					colCache.regionZ = regionZ;
					ChunkAccess prev = IChunkCellsHolder.ACTIVE_CHUNK.get();
					boolean restore = false;
					if (prev != chunk) {
						IChunkCellsHolder.ACTIVE_CHUNK.set(chunk);
						restore = true;
					}
					try {
						return source.getNoiseBiome(quartX, quartY, quartZ);
					} finally {
						if (restore) {
							if (prev != null) {
								IChunkCellsHolder.ACTIVE_CHUNK.set(prev);
							} else {
								IChunkCellsHolder.ACTIVE_CHUNK.remove();
							}
						}
					}
				});
			}
		}
		return blender.getBiomeResolver(source);
	}
}
