package etcodehome.freeterraforged.world.worldgen.runtime;

public final class BiomeCellCache<O> {
	private static final int CAPACITY = 1024;

	private final OwnerThreadCache<WorldgenPlans.SpatialResult> values =
		new OwnerThreadCache<>(CAPACITY);

	public WorldgenPlans.SpatialResult find(O owner, int quartX, int quartZ) {
		return this.values.get(key(quartX, quartZ), owner);
	}

	public void store(O owner, int quartX, int quartZ, WorldgenPlans.SpatialResult value) {
		if (value != null) {
			this.values.store(key(quartX, quartZ), owner, value);
		}
	}

	public void clear() {
		this.values.clear();
	}

	static int slot(int quartX, int quartZ) {
		return OwnerThreadCache.slot(key(quartX, quartZ), CAPACITY);
	}

	private static long key(int quartX, int quartZ) {
		return ((long) quartX << 32) ^ (quartZ & 0xFFFFFFFFL);
	}
}
