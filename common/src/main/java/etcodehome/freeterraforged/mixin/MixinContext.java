package etcodehome.freeterraforged.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import net.minecraft.world.level.levelgen.material.MaterialRuleContext;
import etcodehome.freeterraforged.world.worldgen.surface.ISurfaceContext;

@Mixin(MaterialRuleContext.class)
public class MixinContext implements ISurfaceContext {
	@Shadow private int stoneDepthAbove;
	@Shadow private int stoneDepthBelow;

	@Override
	public int freeterraforged$getStoneDepthAbove() {
		return this.stoneDepthAbove;
	}

	@Override
	public int freeterraforged$getStoneDepthBelow() {
		return this.stoneDepthBelow;
	}
}
