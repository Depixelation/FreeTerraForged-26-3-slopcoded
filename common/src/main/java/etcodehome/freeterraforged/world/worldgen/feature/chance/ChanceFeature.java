package etcodehome.freeterraforged.world.worldgen.feature.chance;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public class ChanceFeature implements Feature {
	public static final MapCodec<ChanceFeature> CODEC = Config.MAP_CODEC.xmap(ChanceFeature::new, ChanceFeature::config);

	private final Config config;

	public ChanceFeature(Config config) {
		this.config = config;
	}

	public Config config() {
		return this.config;
	}

	@Override
	public MapCodec<ChanceFeature> codec() {
		return CODEC;
	}

	@Override
	public boolean place(WorldGenLevel level, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
		int entryCount = this.config.entries.size();
		ChanceContext chanceCtx = ChanceContext.make(entryCount);
        for (int i = 0; i < entryCount; i++) {
            Entry entry = this.config.entries.get(i);
            float chance = entry.getChance(chanceCtx, level, origin);
            chanceCtx.record(i, chance);
        }

        int index = chanceCtx.nextIndex(random);
        if (index > -1) {
            return this.config.entries.get(index).feature.value().place(level, chunkGenerator, random, origin);
        }
		return false;
	}
	
	public record Entry(Holder<PlacedFeature> feature, float chance, List<ChanceModifier> modifiers) {
		public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			PlacedFeature.CODEC.fieldOf("feature").forGetter(Entry::feature),
			Codec.FLOAT.fieldOf("chance").forGetter(Entry::chance),
			ChanceModifier.CODEC.listOf().fieldOf("modifiers").forGetter(Entry::modifiers)
		).apply(instance, Entry::new));
		
		public float getChance(ChanceContext chanceCtx, WorldGenLevel level, BlockPos origin) {
			float chance = this.chance;
			for (ChanceModifier modifier : this.modifiers) {
				chance *= modifier.getChance(chanceCtx, level, origin);
			}
			return chance;
		}
	}
	
	public record Config(List<Entry> entries) {
		public static final MapCodec<Config> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			Entry.CODEC.listOf().fieldOf("entries").forGetter(Config::entries)
		).apply(instance, Config::new));
		public static final Codec<Config> CODEC = MAP_CODEC.codec();
	}
}
