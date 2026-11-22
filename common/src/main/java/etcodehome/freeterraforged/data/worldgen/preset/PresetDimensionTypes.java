package etcodehome.freeterraforged.data.worldgen.preset;

import java.util.Optional;

import etcodehome.freeterraforged.data.worldgen.preset.settings.Preset;
import etcodehome.freeterraforged.data.worldgen.preset.settings.WorldSettings;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.biome.OverworldBiomes;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TimelineTags;
import net.minecraft.util.ARGB;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.attribute.AmbientSounds;
import net.minecraft.world.attribute.BackgroundMusic;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.attribute.EnvironmentAttributeMap;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.clock.WorldClocks;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.timeline.Timeline;

public final class PresetDimensionTypes {
	
	public static void bootstrap(Preset preset, BootstrapContext<DimensionType> ctx) {
		WorldSettings worldSettings = preset.world();
		WorldSettings.Properties properties = worldSettings.properties;
		int worldHeight = properties.worldHeight;
		int worldDepth = properties.worldDepth;
		int totalHeight = worldDepth + worldHeight;

		HolderGetter<Block> blocks = ctx.lookup(Registries.BLOCK);
		HolderGetter<Timeline> timelines = ctx.lookup(Registries.TIMELINE);
		HolderGetter<WorldClock> worldClocks = ctx.lookup(Registries.WORLD_CLOCK);

		EnvironmentAttributeMap attributes = EnvironmentAttributeMap.builder()
			.set(EnvironmentAttributes.FOG_COLOR, ARGB.vector3fFromRGB24(-4138753))
			.set(EnvironmentAttributes.SKY_COLOR, ARGB.vector3fFromRGB24(OverworldBiomes.calculateSkyColor(0.8f)))
			.set(EnvironmentAttributes.AMBIENT_LIGHT_COLOR, ARGB.vector3fFromRGB24(-16119286))
			.set(EnvironmentAttributes.CLOUD_COLOR, ARGB.vector4fFromARGB32(ARGB.white(0.8f)))
			.set(EnvironmentAttributes.CLOUD_HEIGHT, 192.33f)
			.set(EnvironmentAttributes.BACKGROUND_MUSIC, BackgroundMusic.OVERWORLD)
			.set(EnvironmentAttributes.BED_RULE, BedRule.CAN_SLEEP_WHEN_DARK)
			.set(EnvironmentAttributes.STRAW_BED_RULE, BedRule.DESTROY_ON_LEAVE)
			.set(EnvironmentAttributes.RESPAWN_ANCHOR_WORKS, false)
			.set(EnvironmentAttributes.NETHER_PORTAL_SPAWNS_PIGLINS, true)
			.set(EnvironmentAttributes.AMBIENT_SOUNDS, AmbientSounds.LEGACY_CAVE_SETTINGS)
			.build();
		
		ctx.register(BuiltinDimensionTypes.OVERWORLD, new DimensionType(
			false,
			true,
			false,
			false,
			1.0,
			-worldDepth,
			totalHeight,
			totalHeight,
			blocks.getOrThrow(BlockTags.INFINIBURN_OVERWORLD),
			0.0f,
			new DimensionType.MonsterSettings(UniformInt.of(0, 7), 0),
			DimensionType.Skybox.OVERWORLD,
			CardinalLighting.Type.DEFAULT,
			attributes,
			timelines.getOrThrow(TimelineTags.IN_OVERWORLD),
			Optional.of(worldClocks.getOrThrow(WorldClocks.OVERWORLD))
		));
	}
}
