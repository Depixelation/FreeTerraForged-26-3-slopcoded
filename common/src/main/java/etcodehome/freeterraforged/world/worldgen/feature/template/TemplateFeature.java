package etcodehome.freeterraforged.world.worldgen.feature.template;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import etcodehome.freeterraforged.FTFCommon;
import etcodehome.freeterraforged.server.FTFMinecraftServer;
import etcodehome.freeterraforged.world.worldgen.feature.template.decorator.DecoratorConfig;
import etcodehome.freeterraforged.world.worldgen.feature.template.decorator.TemplateDecorator;
import etcodehome.freeterraforged.world.worldgen.feature.template.paste.Paste;
import etcodehome.freeterraforged.world.worldgen.feature.template.paste.PasteConfig;
import etcodehome.freeterraforged.world.worldgen.feature.template.paste.PasteType;
import etcodehome.freeterraforged.world.worldgen.feature.template.placement.TemplatePlacement;
import etcodehome.freeterraforged.world.worldgen.feature.template.template.Dimensions;
import etcodehome.freeterraforged.world.worldgen.feature.template.template.FeatureTemplate;
import etcodehome.freeterraforged.world.worldgen.feature.template.template.TemplateContext;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

public class TemplateFeature implements Feature {
	public static final MapCodec<TemplateFeature> CODEC = Config.MAP_CODEC.xmap(TemplateFeature::new, TemplateFeature::config);

	private final Config<?> config;

	public TemplateFeature(Config<?> config) {
		this.config = config;
	}

	public Config<?> config() {
		return this.config;
	}

	@Override
	public MapCodec<TemplateFeature> codec() {
		return CODEC;
	}

	@Override
	public boolean place(WorldGenLevel level, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
		Mirror mirror = nextMirror(random);
		Rotation rotation = nextRotation(random);
        return paste(level, random, origin, mirror, rotation, this.config, FeatureTemplate.WORLD_GEN);
	}

    public static <T extends TemplateContext> boolean paste(WorldGenLevel world, RandomSource rand, BlockPos pos, Mirror mirror, Rotation rotation, Config<T> config, PasteType pasteType) {
        return paste(world, rand, pos, mirror, rotation, config, pasteType, false);
    }

    public static <T extends TemplateContext> boolean paste(WorldGenLevel world, RandomSource rand, BlockPos pos, Mirror mirror, Rotation rotation, Config<T> config, PasteType pasteType, boolean modified) {
        if (config.templates().isEmpty()) {
            FTFCommon.LOGGER.warn("Empty template list for config");
            return false;
        }
        
        if(world.getServer() instanceof FTFMinecraftServer ftfMinecraftServer) {
	        DecoratorConfig<T> decoratorConfig = config.decorator();
	        
	        Identifier templateName = nextTemplate(config.templates, rand);

			// safely load features and fail verbosely otherwise
	        FeatureTemplate template = ftfMinecraftServer.getFeatureTemplateManager().load(templateName);
			if (template == null) {
				FTFCommon.LOGGER.error("Template failed to load: " + templateName);
				return false;
			}
	        
	        Dimensions dimensions = template.getDimensions(mirror, rotation);
	        TemplatePlacement<T> placement = config.placement();
	        if (!placement.canPlaceAt(world, pos, dimensions)) {
	            return false;
	        }
	
	        Paste paste = pasteType.get(template);
	        T buffer = placement.createContext();
	        if (paste.apply(world, buffer, pos, mirror, rotation, placement, config.paste())) {
	            Identifier biome = world.getBiome(pos).unwrapKey().map(ResourceKey::identifier).orElse(null);
	            for (TemplateDecorator<T> decorator : decoratorConfig.getDecorators(biome)) {
	                decorator.apply(world, buffer, rand, modified);
	            }
	            return true;
	        }
	
	        return false;
        } else {
        	throw new IllegalStateException();
        }
    }

	private static Identifier nextTemplate(List<Identifier> templates, RandomSource random) {
        return templates.get(random.nextInt(templates.size()));
    }

    private static Mirror nextMirror(RandomSource random) {
        return Mirror.values()[random.nextInt(Mirror.values().length)];
    }

    private static Rotation nextRotation(RandomSource random) {
        return Rotation.values()[random.nextInt(Rotation.values().length)];
    }
    
	public record Config<T extends TemplateContext>(List<Identifier> templates, TemplatePlacement<T> placement, PasteConfig paste, DecoratorConfig<T> decorator) {
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public static final MapCodec<Config<?>> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			Identifier.CODEC.listOf().fieldOf("templates").forGetter(Config::templates),
			TemplatePlacement.CODEC.fieldOf("placement").forGetter(Config::placement),
			PasteConfig.CODEC.fieldOf("paste").forGetter(Config::paste),
			DecoratorConfig.CODEC.fieldOf("decorator").forGetter(Config::decorator)
		).apply(instance, (templates, placement, paste, decorator) -> new Config(templates, placement, paste, decorator)));
		public static final Codec<Config<?>> CODEC = MAP_CODEC.codec();
	}
}
