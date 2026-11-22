package etcodehome.freeterraforged.mixin;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.EnvironmentScanPlacement;
import net.minecraft.world.level.levelgen.placement.FeaturePlacer;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.OffsetPlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.RarityFilter;

import etcodehome.freeterraforged.world.worldgen.feature.ore.DynamicOrePlan.FanoutStage;
import etcodehome.freeterraforged.world.worldgen.feature.ore.DynamicOrePlacement;
import etcodehome.freeterraforged.world.worldgen.feature.placement.ChunkLocalFeaturePlacement;
import etcodehome.freeterraforged.world.worldgen.feature.placement.DynamicHeightRangePlacement;
import etcodehome.freeterraforged.world.worldgen.feature.placement.SurfaceFeatureRescue;

@Mixin(FeaturePlacer.class)
public abstract class MixinFeaturePlacer {

	@Shadow @Final private WorldGenLevel level;
	@Shadow @Final private ChunkGenerator generator;

	@WrapMethod(method = "place(Lnet/minecraft/world/level/levelgen/placement/PlacedFeature;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;Z)Z")
	private boolean freeterraforged$manageFeatureScopes(
		PlacedFeature feature,
		RandomSource random,
		BlockPos origin,
		boolean checkBiome,
		Operation<Boolean> original
	) {
		PlacementContext context = new PlacementContext(this.level, this.generator, checkBiome ? Optional.of(feature) : Optional.empty());
		boolean chunkLocalScope = ChunkLocalFeaturePlacement.begin(feature, context, origin);
		try {
			boolean rescueScope = SurfaceFeatureRescue.begin(feature, context);
			try {
				return original.call(feature, random, origin, checkBiome);
			} finally {
				SurfaceFeatureRescue.finish(rescueScope);
			}
		} finally {
			ChunkLocalFeaturePlacement.finish(chunkLocalScope);
		}
	}

	@WrapOperation(
		method = "place(Lnet/minecraft/world/level/levelgen/placement/PlacedFeature;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;Z)Z",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/levelgen/placement/PlacementModifier;modify(Lnet/minecraft/world/level/levelgen/placement/PlacementContext;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;Ljava/util/function/Consumer;)V"
		)
	)
	private void freeterraforged$modifyPlacement(
		PlacementModifier modifier,
		PlacementContext context,
		RandomSource random,
		BlockPos pos,
		Consumer<BlockPos> consumer,
		Operation<Void> original
	) {
		// 1. HeightRangePlacement
		if (modifier instanceof HeightRangePlacement heightRange) {
			var orePositions = DynamicOrePlacement.getHeightPositions(heightRange, context, random, pos);
			if (orePositions.isPresent()) {
				orePositions.get().forEach(consumer);
				return;
			}
			if (DynamicOrePlacement.isStandardOrePlacement(heightRange, context)) {
				original.call(modifier, context, random, pos, consumer);
				return;
			}
			var dhrPositions = DynamicHeightRangePlacement.getPositions(heightRange, context, random, pos);
			if (dhrPositions.isPresent()) {
				dhrPositions.get().forEach(consumer);
				return;
			}
			original.call(modifier, context, random, pos, consumer);
			return;
		}

		// 2. InSquarePlacement
		if (modifier instanceof InSquarePlacement inSquare) {
			var inSquarePositions = DynamicOrePlacement.getInSquarePositions(inSquare, context, random, pos);
			if (inSquarePositions.isPresent()) {
				inSquarePositions.get().forEach(consumer);
				return;
			}
			original.call(modifier, context, random, pos, consumer);
			return;
		}

		// 3. CountPlacement
		if (modifier instanceof CountPlacement countPlacement) {
			int count = countPlacement.count(random, pos);
			SurfaceFeatureRescue.recordCount(countPlacement, count);
			Stream<BlockPos> authored = IntStream.range(0, count).mapToObj(ignored -> pos);
			var fanout = DynamicOrePlacement.getFanoutPositions(countPlacement, context, random, authored, FanoutStage.COUNT);
			if (fanout.isPresent()) {
				fanout.get().forEach(consumer);
				return;
			}
			for (int i = 0; i < count; i++) {
				consumer.accept(pos);
			}
			return;
		}

		// 4. RarityFilter
		if (modifier instanceof RarityFilter rarityFilter) {
			boolean shouldPlace = rarityFilter.shouldPlace(context, random, pos);
			Stream<BlockPos> authored = shouldPlace ? Stream.of(pos) : Stream.empty();
			var fanout = DynamicOrePlacement.getFanoutPositions(rarityFilter, context, random, authored, FanoutStage.RARITY);
			if (fanout.isPresent()) {
				fanout.get().forEach(consumer);
				return;
			}
			if (shouldPlace) {
				consumer.accept(pos);
			}
			return;
		}

		// 5. OffsetPlacement
		if (modifier instanceof OffsetPlacement offsetPlacement) {
			original.call(modifier, context, random, pos, (Consumer<BlockPos>) outputPos -> {
				ChunkLocalFeaturePlacement.constrain(offsetPlacement, Stream.of(outputPos)).forEach(consumer);
			});
			return;
		}

		// 6. EnvironmentScanPlacement
		if (modifier instanceof EnvironmentScanPlacement envScan) {
			boolean[] accepted = new boolean[1];
			original.call(modifier, context, random, pos, (Consumer<BlockPos>) outputPos -> {
				accepted[0] = true;
				consumer.accept(outputPos);
			});
			if (!accepted[0]) {
				SurfaceFeatureRescue.rescue(envScan, pos, Stream.empty()).forEach(consumer);
			}
			return;
		}

		// Default fallback
		original.call(modifier, context, random, pos, consumer);
	}
}
