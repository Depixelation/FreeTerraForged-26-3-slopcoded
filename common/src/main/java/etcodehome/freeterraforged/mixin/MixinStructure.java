package etcodehome.freeterraforged.mixin;

import etcodehome.freeterraforged.world.worldgen.runtime.TerraForgedChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.level.levelgen.structure.Structure;

@Mixin(Structure.GenerationContext.class)
public class MixinStructure {

	@Inject(
		at = @At("HEAD"), 
		method = "isValidBiome",
		cancellable = true
	)
	private void isValidBiome(Structure.GenerationStub generationStub, CallbackInfoReturnable<Boolean> callback) {
		Structure.GenerationContext generationContext = (Structure.GenerationContext) (Object) this;
		if (!(generationContext.chunkGenerator() instanceof TerraForgedChunkGenerator generator)) {
			return;
		}
		for (var rule : generator.activeStructurePlan().rules()) {
			if (!rule.value().test(generationContext.randomState(), generationStub.position())) {
				callback.setReturnValue(false);
				return;
			}
		}
	}
}
