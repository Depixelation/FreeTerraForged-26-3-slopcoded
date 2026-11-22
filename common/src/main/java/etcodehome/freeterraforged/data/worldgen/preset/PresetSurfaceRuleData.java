package etcodehome.freeterraforged.data.worldgen.preset;

import java.util.ArrayList;
import java.util.List;

import etcodehome.freeterraforged.data.worldgen.preset.settings.Preset;
import etcodehome.freeterraforged.tags.FTFBlockTags;
import etcodehome.freeterraforged.world.worldgen.surface.rule.FTFSurfaceRules;
import etcodehome.freeterraforged.world.worldgen.surface.rule.StrataRule;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.data.worldgen.material.OverworldMaterialRules;
import net.minecraft.world.level.levelgen.material.MaterialRules;
import net.minecraft.world.level.levelgen.material.rule.MaterialRule;
import etcodehome.freeterraforged.FTFCommon;
import etcodehome.freeterraforged.world.worldgen.noise.module.Noise;

public class PresetSurfaceRuleData {
    
    public static MaterialRule overworld(Preset preset, HolderGetter<MaterialRule> materialRules, HolderGetter<Noise> noise) {
		MaterialRule overworld = MaterialRules.getRule(materialRules, OverworldMaterialRules.OVERWORLD);
		var base = new etcodehome.freeterraforged.world.worldgen.surface.rule.InheritedSurfaceRule(overworld,
			etcodehome.freeterraforged.world.worldgen.surface.rule.InheritedSurfaceRule.BIOME_DOMAINS);
		if (preset.miscellaneous().strataDecorator) {
			return MaterialRules.sequence(base, makeStrataRule(noise));
		}
		return MaterialRules.sequence(base);
    }
    
	private static MaterialRule makeStrataRule(HolderGetter<Noise> noise) {
		Holder<Noise> depth = noise.getOrThrow(PresetStrataNoise.STRATA_DEPTH);

		List<StrataRule.Strata> strata = new ArrayList<>();
		strata.add(new StrataRule.Strata(FTFBlockTags.SOIL, depth, 3, 0, 1, 0.1F, 0.25F));
		strata.add(new StrataRule.Strata(FTFBlockTags.SEDIMENT, depth, 3, 0, 2, 0.05F, 0.15F));
		strata.add(new StrataRule.Strata(FTFBlockTags.CLAY, depth, 3, 0, 2, 0.05F, 0.1F));
		strata.add(new StrataRule.Strata(FTFBlockTags.ROCK, depth, 3, 10, 30, 0.1F, 1.5F));
		return FTFSurfaceRules.strata(FTFCommon.location("overworld_strata"), noise.getOrThrow(PresetStrataNoise.STRATA_SELECTOR), strata, 100);
	}
}
