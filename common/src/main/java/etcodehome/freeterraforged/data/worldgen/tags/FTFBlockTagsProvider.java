package etcodehome.freeterraforged.data.worldgen.tags;

import java.util.concurrent.CompletableFuture;

import etcodehome.freeterraforged.data.worldgen.preset.settings.Preset;
import etcodehome.freeterraforged.tags.FTFBlockTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagAppender;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class FTFBlockTagsProvider extends TagsProvider<Block> {
	private Preset preset;
	
	public FTFBlockTagsProvider(Preset preset, PackOutput packOutput, CompletableFuture<Provider> completableFuture) {
		super(packOutput, Registries.BLOCK, completableFuture);

		this.preset = preset;
	}

	private TagAppender<Block> addBlocks(TagKey<Block> tag, Block... blocks) {
		TagAppender<Block> appender = this.tag(tag);
		for (Block block : blocks) {
			appender.add(BuiltInRegistries.BLOCK.getResourceKey(block).orElseThrow());
		}
		return appender;
	}

	@Override
	protected void addTags(HolderLookup.Provider provider) {
//		MiscellaneousSettings miscellaneousSettings = this.preset.miscellaneous();

		this.addBlocks(FTFBlockTags.SOIL, Blocks.DIRT, Blocks.COARSE_DIRT).addOptionalTag(BlockTags.DIRT);
		this.addBlocks(FTFBlockTags.CLAY, Blocks.CLAY);
		this.addBlocks(FTFBlockTags.SEDIMENT, Blocks.SAND, Blocks.GRAVEL);
		this.addBlocks(FTFBlockTags.ERODIBLE, Blocks.SNOW_BLOCK, Blocks.POWDER_SNOW, Blocks.GRAVEL)
				.addOptionalTag(BlockTags.DIRT)
				.addOptionalTag(BlockTags.GRASS_BLOCKS)
				.addOptionalTag(BlockTags.SUBSTRATE_OVERWORLD)
				.addOptionalTag(BlockTags.SUPPORTS_VEGETATION);
		
//		if(!miscellaneousSettings.oreCompatibleStoneOnly) {
			this.addBlocks(FTFBlockTags.ROCK, Blocks.GRANITE, Blocks.ANDESITE, Blocks.STONE, Blocks.DIORITE);
//		} else{
			//TODO
//		}
	}
}
