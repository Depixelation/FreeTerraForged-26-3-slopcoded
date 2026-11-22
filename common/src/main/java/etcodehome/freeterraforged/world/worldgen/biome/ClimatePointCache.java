package etcodehome.freeterraforged.world.worldgen.biome;

import net.minecraft.world.level.biome.Climate;
import etcodehome.freeterraforged.world.worldgen.runtime.OwnerThreadCache;

public final class ClimatePointCache {

    private static final int SIZE = 4096; // Power of two for rapid bit-masking
    private final OwnerThreadCache<Climate.TargetPoint> values = new OwnerThreadCache<>(SIZE);

    /**
     * @return the cached climate point for this sampler, or null on a cache miss.
     */
    public Climate.TargetPoint find(final Object sampler, final int x, final int y, final int z) {
        final long key = key(x, y, z);
        return this.values.get(key, sampler);
    }

    /**
     * Stores a fully evaluated climate point in the active sampler slot.
     */
    public void store(final Object sampler, final int x, final int y, final int z, final Climate.TargetPoint target) {
        if (target == null) {
            return;
        }
        final long key = key(x, y, z);
        this.values.store(key, sampler, target);
    }

	public void clear() {
		this.values.clear();
	}

    /**
     * Packs 3D quart coordinates safely into a single 64-bit primitive key.
     * Quart coordinates comfortably fit: |x|,|z| < 2^23, |y| < 2^15.
     */
    private static long key(final int x, final int y, final int z) {
        return ((long) (y & 0xFFFF) << 48) | ((long) (x & 0xFFFFFF) << 24) | (z & 0xFFFFFFL);
    }

    public ClimatePointCache() {}

}
