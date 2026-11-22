package etcodehome.freeterraforged.world.worldgen.surface.rule;

import java.util.List;

import com.mojang.serialization.MapCodec;
import etcodehome.freeterraforged.platform.RegistryUtil;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.material.rule.MaterialRule;
import etcodehome.freeterraforged.world.worldgen.noise.module.Noise;
import etcodehome.freeterraforged.world.worldgen.surface.rule.StrataRule.Strata;

public class FTFSurfaceRules {

	public static void bootstrap() {
		register("strata", StrataRule.CODEC);
		register("inherited_surface", InheritedSurfaceRule.CODEC);
		register("acquired_surface", AcquiredSurfaceRule.CODEC);
	}
	
	public static StrataRule strata(Identifier name, Holder<Noise> selector, List<Strata> strata, int iterations) {
		return new StrataRule(name, selector, strata, iterations);
	}
	
	public static void register(String name, MapCodec<? extends MaterialRule> value) {
		RegistryUtil.register(BuiltInRegistries.MATERIAL_RULE_TYPE, name, value);
	}
}
