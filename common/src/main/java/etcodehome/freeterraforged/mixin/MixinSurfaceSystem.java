package etcodehome.freeterraforged.mixin;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import etcodehome.freeterraforged.world.worldgen.GeneratorContext;
import etcodehome.freeterraforged.world.worldgen.FTFRandomState;
import etcodehome.freeterraforged.world.worldgen.IChunkCellsHolder;
import etcodehome.freeterraforged.world.worldgen.cell.Cell;
import etcodehome.freeterraforged.world.worldgen.cell.rivermap.ContinentalHydrology;
import etcodehome.freeterraforged.world.worldgen.cell.rivermap.river.RiverCarverSettings;
import etcodehome.freeterraforged.world.worldgen.densityfunction.CellSampler;
import etcodehome.freeterraforged.world.worldgen.perf.WorldgenPerfTracker;
import etcodehome.freeterraforged.world.worldgen.surface.FTFSurfaceSystem;
import etcodehome.freeterraforged.world.worldgen.surface.rule.StrataRule;
import java.util.Set;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.material.MaterialSystem;
import net.minecraft.world.level.levelgen.material.rule.MaterialRule;
import etcodehome.freeterraforged.FTFCommon;

@Mixin(MaterialSystem.class)
@Implements(@Interface(iface = FTFSurfaceSystem.class, prefix = FTFCommon.MOD_ID + "$FTFSurfaceSystem$"))
class MixinSurfaceSystem {
	private static final Identifier GEOLOGY_RANDOM = FTFCommon.location("geology");
	private static final int[] NEIGHBOR_X = {0, 0, 1, -1};
	private static final int[] NEIGHBOR_Z = {-1, 1, 0, 0};
	private RandomState randomState;
	private volatile Map<Identifier, List<List<StrataRule.Layer>>> strata;

	@Inject(
			at = @At("TAIL"),
			method = "<init>"
	)
	public void MaterialSystem(RandomState randomState, BlockState blockState, int i, DensityFunction preliminarySurfaceFunction, PositionalRandomFactory positionalRandomFactory, CallbackInfo callback) {
		this.randomState = randomState;
	}

	// Sanitize Y parameter passed into getBand to prevent negative array indexing in Badlands terracotta bands
	@ModifyVariable(
			method = "getBand",
			at = @At("HEAD"),
			ordinal = 1,
			argsOnly = true
	)
	private int freeterraforged$safeYForBadlandsBand(int y) {
		return Math.floorMod(y, 192) + 3840;
	}

	@WrapMethod(method = "buildSurface")
	private void freeterraforged$wrapBuildSurface(
		RandomState randomState,
		BiomeManager biomeManager,
		WorldGenerationContext context,
		ChunkAccess chunk,
		NoiseChunk noiseChunk,
		MaterialRule ruleSource,
		Set<Holder<Biome>> biomes,
		Operation<Void> original
	) {
		IChunkCellsHolder.ACTIVE_CHUNK.set(chunk);
		try {
			original.call(randomState, biomeManager, context, chunk, noiseChunk, ruleSource, biomes);
		} finally {
			IChunkCellsHolder.ACTIVE_CHUNK.remove();
		}
	}

	@ModifyVariable(
			method = "buildSurface",
			at = @At("HEAD"),
			argsOnly = true
	)
	private MaterialRule freeterraforged$optimizeSurfaceRule(MaterialRule ruleSource) {
		if ((Object) this.randomState instanceof FTFRandomState ftfRandomState && ftfRandomState.generatorContext() != null) {
			return etcodehome.freeterraforged.world.worldgen.surface.rule.FastStrataSequenceRule.optimize(ruleSource);
		}
		return ruleSource;
	}

	// INJECT AT HEAD to carve out rivers and lakes before surface rules run
	@Inject(
			method = "buildSurface",
			at = @At("HEAD")
	)
	private void onBuildSurface(RandomState randomState, BiomeManager biomeManager, WorldGenerationContext context, final ChunkAccess chunk, NoiseChunk noiseChunk, MaterialRule ruleSource, Set<Holder<Biome>> biomes, CallbackInfo ci) {
		if ((Object) randomState instanceof FTFRandomState ftfRandomState) {
			GeneratorContext genCtx = ftfRandomState.generatorContext();
			if (genCtx != null) {
				if (!WorldgenPerfTracker.ACTIVE) {
					this.freeterraforged$placeRiverWater(chunk, biomeManager, genCtx, noiseChunk);
				} else {
					long t0 = System.nanoTime();
					this.freeterraforged$placeRiverWater(chunk, biomeManager, genCtx, noiseChunk);
					WorldgenPerfTracker.stats().surfaceRiverWaterNanos += (System.nanoTime() - t0);
				}
			}
		}
	}

	public List<List<StrataRule.Layer>> freeterraforged$FTFSurfaceSystem$getOrCreateStrata(Identifier name, Function<RandomSource, List<List<StrataRule.Layer>>> strata) {
		return this.freeterraforged$strata().computeIfAbsent(name, (k) -> {
			PositionalRandomFactory factory = this.randomState.getOrCreateRandomFactory(GEOLOGY_RANDOM);
			return strata.apply(factory.fromHashOf(k));
		});
	}

	@Unique
	private Map<Identifier, List<List<StrataRule.Layer>>> freeterraforged$strata() {
		Map<Identifier, List<List<StrataRule.Layer>>> current = this.strata;
		if (current != null) {
			return current;
		}
		synchronized (this) {
			current = this.strata;
			if (current == null) {
				current = new ConcurrentHashMap<>();
				this.strata = current;
			}
		}
		return current;
	}

	@Unique
	private void freeterraforged$placeRiverWater(ChunkAccess chunk, BiomeManager biomeManager, GeneratorContext genCtx, NoiseChunk noiseChunk) {
		var chunkPos = chunk.getPos();
		int minBlockX = chunkPos.getMinBlockX();
		int minBlockZ = chunkPos.getMinBlockZ();
		IChunkCellsHolder holder = chunk instanceof IChunkCellsHolder h ? h : null;
		Cell[] chunkCells = holder != null ? holder.freeterraforged$getOrCreateChunkCells(genCtx) : null;

		Cell neighborScratch = new Cell();
		java.util.function.BiFunction<Integer, Integer, Cell> cellGetter = (gx, gz) -> {
			int lx = gx - minBlockX;
			int lz = gz - minBlockZ;
			if (chunkCells != null && lx >= -1 && lx <= 16 && lz >= -1 && lz <= 16
					&& (chunkCells.length >= etcodehome.freeterraforged.world.worldgen.cell.filter.ChunkFilterGrid.HOLDER_ARRAY_LENGTH
						|| (lx >= 0 && lx < 16 && lz >= 0 && lz < 16))) {
				return etcodehome.freeterraforged.world.worldgen.cell.filter.ChunkFilterGrid.getPaddedCell(chunkCells, lx, lz);
			}
			boolean perfActive = WorldgenPerfTracker.isActive();
			WorldgenPerfTracker.ThreadStats ts = perfActive ? WorldgenPerfTracker.threadStats() : null;
			WorldgenPerfTracker.ApplyCellCaller prevCaller = null;
			if (ts != null) {
				prevCaller = ts.currentCaller;
				ts.currentCaller = WorldgenPerfTracker.ApplyCellCaller.SURFACE_RIVER;
			}
			try {
				return CellSampler.getCachedOrLookup(genCtx.lookup, gx, gz, false);
			} finally {
				if (ts != null) {
					ts.currentCaller = prevCaller;
				}
			}
		};

		var levels = genCtx.heightmap.levels();
		float oceanLevel = levels.water;
		int oceanY = levels.scale(oceanLevel);
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		BlockState water = Blocks.WATER.defaultBlockState();
		BlockState flowingWater = water.setValue(BlockStateProperties.LEVEL, 1);
		BlockState stone = Blocks.STONE.defaultBlockState();

		// Iterate over the 16x16 chunk exactly once
		for (int localX = 0; localX < 16; localX++) {
			for (int localZ = 0; localZ < 16; localZ++) {
				int globalX = minBlockX + localX;
				int globalZ = minBlockZ + localZ;

				Cell cell = chunkCells != null
					? chunkCells[localZ * 16 + localX]
					: genCtx.lookup.lookup(globalX, globalZ);
				// Wetland basins always hold water. Their riverWaterLevel is the continental uplift offset,
				// which is exactly 0 on the lowest water table step, so it can't double as the "has water" flag.
				boolean isWaterCell = cell.terrain.isWetland()
						|| ((cell.terrain.isRiver() || cell.terrain.isLake()) && cell.riverWaterLevel > 0);
				int scaledY = levels.scale(cell.height);

				if (isWaterCell) {
					int waterY = levels.scale(
							(ContinentalHydrology.getComplexWaterHeight(
									cell.waterTable,
									cell.globalContinentScale,
									cell.continentSizeModifier)
							) + oceanLevel
					);

					if (waterY < oceanY) waterY = oceanY;

					if (waterY > scaledY) {
						boolean isTransitionColumn = false;
						boolean multiBlockDrop = false;
						int lowestNeighborWaterY = waterY;

						// Check neighbors to determine if we are an edge/waterfall
						for (int i = 0; i < 4; i++) {
							int nx = globalX + NEIGHBOR_X[i];
							int nz = globalZ + NEIGHBOR_Z[i];

							var neighborCell = cellGetter.apply(nx, nz);

							// Only consider the neighbor's water height if it is actually a water cell
							boolean nIsWaterCell = neighborCell.terrain.isWetland()
									|| ((neighborCell.terrain.isRiver() || neighborCell.terrain.isLake()) && neighborCell.riverWaterLevel > 0);
							if (nIsWaterCell) {
								int nWaterY = levels.scale(
										(ContinentalHydrology.getComplexWaterHeight(
												neighborCell.waterTable,
												neighborCell.globalContinentScale,
												neighborCell.continentSizeModifier)
										) + levels.water
								);

								if (nWaterY < oceanY) nWaterY = oceanY;

								if (nWaterY < waterY) {
									isTransitionColumn = true;
									lowestNeighborWaterY = Math.min(lowestNeighborWaterY, nWaterY);
									if (waterY - nWaterY > 1) {
										multiBlockDrop = true;
									}
								}
							}
						}

						// adjust this column to be averaged so it makes the waterfalls more natural like.
						if (isTransitionColumn && multiBlockDrop) {
							scaledY = (scaledY + lowestNeighborWaterY) / 2;
						}

						// Build the water column in our OWN chunk down to the needed depth
						for (int wy = scaledY + 1; wy <= waterY; wy++) {
							boolean isTopBlock = wy == waterY;
							pos.set(globalX, wy, globalZ);

							if (isTopBlock && isTransitionColumn && multiBlockDrop) {
								chunk.setBlockState(pos, water, 0);
								chunk.markPosForPostProcessing(pos);
							}
							else if (isTopBlock && isTransitionColumn && !multiBlockDrop) {
								if (biomeManager.getBiome(pos).value().coldEnoughToSnow(pos, oceanY)) {
									// Place Ice instead of liquid water to keep frozen rivers continuous
									chunk.setBlockState(pos, Blocks.ICE.defaultBlockState(), 0);
								} else {
									chunk.setBlockState(pos, flowingWater, 0);
								}
							}
							else if (!isTopBlock && multiBlockDrop) {
								// If we are dropping multiple blocks, fill the gap down to the lowest neighbor with water instead of stone
								if (wy > lowestNeighborWaterY) {
									chunk.setBlockState(pos, flowingWater, 0);
									chunk.markPosForPostProcessing(pos);
								} else {
									chunk.setBlockState(pos, stone, 0);
								}
							}
							else {
								chunk.setBlockState(pos, water, 0);
								if (isTopBlock) {
									chunk.markPosForPostProcessing(pos);
								}
							}
						}
					}
				} else {

					// GASKET LOGIC
					// Only run gasket logic if this land cell is above ocean level
					if (scaledY >= oceanY) {

						// GASKET LOGIC: Soft fade using riverMask
						int maxNeighborWaterY = scaledY;

						// Check neighbors for water and record the strongest mask influence
						for (int i = 0; i < 4; i++) {
							int nx = globalX + NEIGHBOR_X[i];
							int nz = globalZ + NEIGHBOR_Z[i];
							var neighborCell = cellGetter.apply(nx, nz);

							if ((neighborCell.terrain.isRiver() || neighborCell.terrain.isLake())) {
								int nWaterY = levels.scale(
									(ContinentalHydrology.getComplexWaterHeight(
											neighborCell.waterTable,
											neighborCell.globalContinentScale,
											neighborCell.continentSizeModifier)
									) + oceanLevel
								);

								if (nWaterY > maxNeighborWaterY) {
									maxNeighborWaterY = nWaterY;
								}
							}
						}

						// If in river zone and lower than water table, we shouldn't be.
						int waterTableCeil = levels.scale(
							(ContinentalHydrology.getComplexWaterHeight(
									cell.waterTable,
									cell.globalContinentScale,
									cell.continentSizeModifier)
								) + oceanLevel
							);

						boolean isInRiverZone = cell.riverZone == RiverCarverSettings.RiverZone.Banks
								|| cell.riverZone == RiverCarverSettings.RiverZone.ValleyFloor
								|| cell.riverZone == RiverCarverSettings.RiverZone.ValleyFadeout;
						boolean isLowerThanWaterTable = scaledY < waterTableCeil;
						boolean isAboveOcean = scaledY > oceanY;

						if (isInRiverZone && isLowerThanWaterTable && isAboveOcean)  {
							for (int wy = scaledY + 1; wy <= waterTableCeil; wy++) {
								pos.set(globalX, wy, globalZ);
								chunk.setBlockState(pos, stone, 0);
							}
						}

						// If a neighboring water block is higher than our terrain, build a stone pillar up to match it.
						if (maxNeighborWaterY > scaledY) {
							for (int wy = scaledY + 1; wy <= maxNeighborWaterY; wy++) {
								pos.set(globalX, wy, globalZ);
								chunk.setBlockState(pos, stone, 0);
							}
						}
					}
				}
			}
		}
	}
}
