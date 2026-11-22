package etcodehome.freeterraforged.mixin;

import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import etcodehome.freeterraforged.data.worldgen.preset.FTFWorldPresetDataPacks;
import etcodehome.freeterraforged.data.worldgen.preset.FTFWorldPresets;

@Mixin(targets = "net.minecraft.server.dedicated.DedicatedServerProperties$WorldDimensionData")
public abstract class MixinDedicatedServerWorldDimensionData {
	@Redirect(
		method = "create",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/resources/Identifier;tryParse(Ljava/lang/String;)Lnet/minecraft/resources/Identifier;"
		),
		require = 1
	)
	private Identifier freeterraforged$parseWorldTypeAlias(String levelType) {
		if (FTFWorldPresetDataPacks.selectsFreeTerraForged(levelType)) {
			return FTFWorldPresets.FREETERRAFORGED.identifier();
		}
		return Identifier.tryParse(levelType);
	}
}
