package etcodehome.freeterraforged.world.worldgen.floatproviders;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.FloatProvider;

// Kept registered so older datapacks can still decode their legacy canyon provider.
public class LegacyCanyonYScale implements FloatProvider {
	public static final MapCodec<LegacyCanyonYScale> CODEC = MapCodec.unit(LegacyCanyonYScale::new);
	
	@Override
	public float sample(RandomSource random) {
		return (random.nextFloat() - 0.5F) * 2.0F / 8.0F;
	}

	@Override
	public float min() {
		return -1.0F;
	}

	@Override
	public float max() {
		return 1.0F;
	}

	@Override
	public MapCodec<LegacyCanyonYScale> codec() {
		return FTFFloatProviderTypes.LEGACY_CANYON_Y_SCALE;
	}
}
