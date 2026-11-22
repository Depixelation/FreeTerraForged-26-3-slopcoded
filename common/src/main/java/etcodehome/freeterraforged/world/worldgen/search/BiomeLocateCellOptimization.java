package etcodehome.freeterraforged.world.worldgen.search;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.Set;

import etcodehome.freeterraforged.FTFCommon;
import etcodehome.freeterraforged.world.worldgen.GeneratorContext;
import etcodehome.freeterraforged.world.worldgen.cell.Cell;
import etcodehome.freeterraforged.world.worldgen.densityfunction.CellSampler;
import etcodehome.freeterraforged.world.worldgen.densityfunction.MarkerFunction;
import etcodehome.freeterraforged.world.worldgen.runtime.WorldgenBiomeSelection;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.densityfunction.CachingDensitySampler;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.densityfunction.DensitySampler;

/** Proves when a raw cell is already authoritative for one standard FTF biome-locate query. */
final class BiomeLocateCellOptimization {
	private static final Identifier FTF_SELECTION_POLICY = FTFCommon.location("ftf_selection_policy");

	private final GeneratorContext context;
	private final Set<CellSampler.Field> sampledFields;
	private final boolean enabled;

	private BiomeLocateCellOptimization(
		GeneratorContext context,
		Set<CellSampler.Field> sampledFields,
		boolean enabled
	) {
		this.context = context;
		this.sampledFields = Set.copyOf(sampledFields);
		this.enabled = enabled;
	}

	static BiomeLocateCellOptimization compile(
		WorldgenBiomeSelection.Executable selection,
		GeneratorContext context,
		Climate.Sampler sampler
	) {
		var plan = selection.plan();
		boolean standardSelection = plan.providerSelection().directInput().isEmpty()
			&& plan.samplerDecoration().stages().isEmpty()
			&& plan.selectionDecoration().stages().size() == 1
			&& plan.selectionDecoration().stages().getFirst().id().equals(FTF_SELECTION_POLICY);
		EnumSet<CellSampler.Field> sampledFields = EnumSet.noneOf(CellSampler.Field.class);
		collect(sampledFields, sampler.temperature());
		collect(sampledFields, sampler.humidity());
		collect(sampledFields, sampler.continentalness());
		collect(sampledFields, sampler.erosion());
		collect(sampledFields, sampler.depth());
		collect(sampledFields, sampler.weirdness());
		return new BiomeLocateCellOptimization(context, sampledFields, standardSelection);
	}

	boolean canUseRaw(Cell cell) {
		return this.enabled && (this.context.filters == null
			|| this.context.filters.preservesRawBiomeLocateInputs(cell, this.sampledFields));
	}

	boolean enabled() {
		return this.enabled;
	}

	void assertEquivalent(Cell raw, Cell filtered, int blockX, int blockZ) {
		assertEqual(blockX, blockZ, "height", raw.height, filtered.height);
		if (raw.biomeRegionX != filtered.biomeRegionX || raw.biomeRegionZ != filtered.biomeRegionZ) {
			throw mismatch(blockX, blockZ, "biome-region ownership", raw, filtered);
		}
		if (raw.terrain.isRiver() != filtered.terrain.isRiver()) {
			throw mismatch(blockX, blockZ, "river terrain role", raw.terrain, filtered.terrain);
		}
		for (CellSampler.Field field : this.sampledFields) {
			assertEqual(
				blockX,
				blockZ,
				field.getSerializedName(),
				field.readFinite(raw, this.context.heightmap),
				field.readFinite(filtered, this.context.heightmap)
			);
		}
	}

	private static void collect(EnumSet<CellSampler.Field> fields, DensitySampler.Bound root) {
		collectSampler(fields, root.sampler(), Collections.newSetFromMap(new IdentityHashMap<>()));
	}

	private static void collectSampler(
		EnumSet<CellSampler.Field> fields,
		Object node,
		Set<Object> visited
	) {
		if (node == null || !visited.add(node)) {
			return;
		}
		if (node instanceof CellSampler sampler) {
			fields.add(sampler.field());
			return;
		}
		if (node instanceof CellSampler.CacheChunk sampler) {
			fields.add(sampler.field());
			return;
		}
		if (node instanceof CachingDensitySampler caching) {
			collectSampler(fields, caching.input(), visited);
			return;
		}
		if (node instanceof DensityFunction df) {
			MarkerFunction.mapAll(df, function -> {
				if (function instanceof CellSampler sampler) {
					fields.add(sampler.field());
				} else if (function instanceof CellSampler.CacheChunk sampler) {
					fields.add(sampler.field());
				}
				return function;
			});
			return;
		}
		if (node instanceof DensitySampler) {
			for (Class<?> cls = node.getClass(); cls != null && cls != Object.class; cls = cls.getSuperclass()) {
				for (Field field : cls.getDeclaredFields()) {
					if (Modifier.isStatic(field.getModifiers())) continue;
					try {
						field.setAccessible(true);
						Object val = field.get(node);
						if (val instanceof DensitySampler || val instanceof DensityFunction) {
							collectSampler(fields, val, visited);
						} else if (val instanceof Object[] arr) {
							for (Object item : arr) {
								if (item instanceof DensitySampler || item instanceof DensityFunction) {
									collectSampler(fields, item, visited);
								}
							}
						}
					} catch (ReflectiveOperationException | RuntimeException ignored) {
					}
				}
			}
		}
	}

	private static void assertEqual(
		int blockX,
		int blockZ,
		String field,
		float expected,
		float actual
	) {
		if (Float.floatToIntBits(expected) != Float.floatToIntBits(actual)) {
			throw mismatch(blockX, blockZ, field, expected, actual);
		}
	}

	private static AssertionError mismatch(
		int blockX,
		int blockZ,
		String field,
		Object expected,
		Object actual
	) {
		return new AssertionError(
			"Raw biome-locate optimization disagrees with filtered terrain at " + blockX + "," + blockZ
				+ " for " + field + ": expected " + expected + ", got " + actual
		);
	}
}
