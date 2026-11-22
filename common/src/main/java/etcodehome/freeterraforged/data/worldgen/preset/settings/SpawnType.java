package etcodehome.freeterraforged.data.worldgen.preset.settings;

import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;

import etcodehome.freeterraforged.world.worldgen.GeneratorContext;
import etcodehome.freeterraforged.world.worldgen.util.PosUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.Climate.ParameterPoint;
import net.minecraft.world.level.biome.OverworldBiomeBuilder;
import net.minecraft.world.level.levelgen.NoiseRouterData;
import net.minecraft.world.level.levelgen.SpawnTargetPoint;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;

public enum SpawnType implements StringRepresentable {

    CONTINENT_CENTER("CONTINENT_CENTER") {
        private static final Climate.Parameter FULL_RANGE = Climate.Parameter.span(-1.0F, 1.0F);
        private static final Climate.Parameter SURFACE_DEPTH = Climate.Parameter.point(0.0F);
        private static final Climate.Parameter INLAND_CONTINENTALNESS = Climate.Parameter.span(-0.11F, 0.55F);

		@Override
    	public BlockPos getSearchCenter(GeneratorContext ctx, WorldSettings.Properties properties) {
    		long center = ctx.heightmap.continent().getNearestCenter(0.0F, 0.0F);
    		return new BlockPos(PosUtil.unpackLeft(center), 0, PosUtil.unpackRight(center));
    	}
    	
		@Override
		public List<ParameterPoint> getParameterPoints() {
			return List.of(new Climate.ParameterPoint(FULL_RANGE, FULL_RANGE, Climate.Parameter.span(INLAND_CONTINENTALNESS, FULL_RANGE), FULL_RANGE, SURFACE_DEPTH, FULL_RANGE, 0L), new Climate.ParameterPoint(FULL_RANGE, FULL_RANGE, Climate.Parameter.span(INLAND_CONTINENTALNESS, FULL_RANGE), FULL_RANGE, SURFACE_DEPTH, FULL_RANGE, 0L));
		}

		@Override
		public List<SpawnTargetPoint> getSpawnTargets(HolderGetter<DensityFunction> densityFunctions) {
			Climate.Parameter inland = Climate.Parameter.span(INLAND_CONTINENTALNESS, FULL_RANGE);
			return List.of(new SpawnTargetPoint(Map.of(
				densityFunctions.getOrThrow(NoiseRouterData.OVERWORLD_FUNCTIONS.temperature()), FULL_RANGE,
				densityFunctions.getOrThrow(NoiseRouterData.OVERWORLD_FUNCTIONS.vegetation()), FULL_RANGE,
				densityFunctions.getOrThrow(NoiseRouterData.OVERWORLD_FUNCTIONS.continents()), inland,
				densityFunctions.getOrThrow(NoiseRouterData.OVERWORLD_FUNCTIONS.erosion()), FULL_RANGE,
				densityFunctions.getOrThrow(NoiseRouterData.OVERWORLD_FUNCTIONS.depth()), SURFACE_DEPTH,
				densityFunctions.getOrThrow(NoiseRouterData.RIDGES), FULL_RANGE
			)));
		}
	}, 
    ISLANDS("ISLANDS") {

		@Override
		public BlockPos getSearchCenter(GeneratorContext ctx, WorldSettings.Properties properties) {
			return BlockPos.ZERO;
		}

		@Override
		public List<ParameterPoint> getParameterPoints() {
			return ImmutableList.of();
		}

		@Override
		public List<SpawnTargetPoint> getSpawnTargets(HolderGetter<DensityFunction> densityFunctions) {
			return ImmutableList.of();
		}
	},
    WORLD_ORIGIN("WORLD_ORIGIN") {

		@Override
		public BlockPos getSearchCenter(GeneratorContext ctx, WorldSettings.Properties properties) {
			return BlockPos.ZERO;
		}

		@Override
		public List<ParameterPoint> getParameterPoints() {
			return ImmutableList.of();
		}

		@Override
		public List<SpawnTargetPoint> getSpawnTargets(HolderGetter<DensityFunction> densityFunctions) {
			return ImmutableList.of();
		}
	},
	USER_SELECTED("USER_SELECTED") {

		// Define a range that covers all possible values to ensure the point is never "out of bounds"
		private static final Climate.Parameter FULL_RANGE = Climate.Parameter.span(-2.0F, 2.0F);

		@Override
		public BlockPos getSearchCenter(GeneratorContext ctx, WorldSettings.Properties properties) {
			BlockPos userDefined = new BlockPos(properties.spawnX, 0, properties.spawnZ);
			return userDefined;
		}

		@Override
		public List<ParameterPoint> getParameterPoints() {
			// force the vanilla logic to respect that this coordinate is a valid surface spawn
			return List.of(new Climate.ParameterPoint(
					FULL_RANGE, // Temperature
					FULL_RANGE, // Humidity
					FULL_RANGE, // Continentalness
					FULL_RANGE, // Erosion
					FULL_RANGE, // Depth
					FULL_RANGE, // Weirdness
					0L          // Offset
			));
		}

		@Override
		public List<SpawnTargetPoint> getSpawnTargets(HolderGetter<DensityFunction> densityFunctions) {
			return List.of(new SpawnTargetPoint(Map.of(
				densityFunctions.getOrThrow(NoiseRouterData.OVERWORLD_FUNCTIONS.continents()), FULL_RANGE
			)));
		}
	};
	
	public static final Codec<SpawnType> CODEC = StringRepresentable.fromEnum(SpawnType::values);

	private String name;
	
	private SpawnType(String name) {
		this.name = name;
	}
	
	@Override
	public String getSerializedName() {
		return this.name;
	}
	
	public abstract BlockPos getSearchCenter(GeneratorContext ctx, WorldSettings.Properties properties);
	
	public abstract List<ParameterPoint> getParameterPoints();

	public abstract List<SpawnTargetPoint> getSpawnTargets(HolderGetter<DensityFunction> densityFunctions);
}