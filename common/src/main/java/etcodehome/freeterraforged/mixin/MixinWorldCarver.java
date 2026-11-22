package etcodehome.freeterraforged.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import etcodehome.freeterraforged.world.worldgen.FTFRandomState;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(NoiseBasedChunkGenerator.class)
public class MixinWorldCarver {
	@WrapOperation(
		method = "lambda$applyCarvingMask$0",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/tags/TagKey;)Z"
		)
	)
	private static boolean freeterraforged$preventWaterCarving(
		BlockState state,
		TagKey<Block> tag,
		Operation<Boolean> original,
		@Local(argsOnly = true) RandomState randomState
	) {
		if (original.call(state, tag)) {
			return true;
		}
		if ((Object) randomState instanceof FTFRandomState ftfRandomState
			&& ftfRandomState.isTerraForged()
			&& (state.getFluidState().isSourceOfType(Fluids.WATER)
				|| state.getFluidState().isSourceOfType(Fluids.FLOWING_WATER))) {
			return true;
		}
		return false;
	}
}
