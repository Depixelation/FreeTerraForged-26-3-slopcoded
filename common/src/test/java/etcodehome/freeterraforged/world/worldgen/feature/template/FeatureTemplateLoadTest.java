package etcodehome.freeterraforged.world.worldgen.feature.template;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import etcodehome.freeterraforged.world.worldgen.feature.placement.FTFPlacementModifiers;
import etcodehome.freeterraforged.world.worldgen.feature.placement.poisson.DensityNoise;
import etcodehome.freeterraforged.world.worldgen.feature.placement.poisson.FastPoisson;
import etcodehome.freeterraforged.world.worldgen.feature.placement.poisson.FastPoissonContext;
import etcodehome.freeterraforged.world.worldgen.feature.placement.poisson.FastPoissonModifier;
import etcodehome.freeterraforged.world.worldgen.feature.template.decorator.TreeContext;
import etcodehome.freeterraforged.world.worldgen.feature.template.paste.PasteConfig;
import etcodehome.freeterraforged.world.worldgen.feature.template.placement.TemplatePlacement;
import etcodehome.freeterraforged.world.worldgen.feature.template.placement.TemplatePlacements;
import etcodehome.freeterraforged.world.worldgen.feature.template.template.BakedTemplate;
import etcodehome.freeterraforged.world.worldgen.feature.template.template.BlockInfo;
import etcodehome.freeterraforged.world.worldgen.feature.template.template.Dimensions;
import etcodehome.freeterraforged.world.worldgen.feature.template.template.FeatureTemplate;
import etcodehome.freeterraforged.world.worldgen.noise.module.Noise;
import etcodehome.freeterraforged.world.worldgen.noise.module.Noises;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FeatureTemplateLoadTest {

	@BeforeAll
	static void setUp() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	void testFastPoissonPoints() {
		FastPoisson poisson = new FastPoisson();
		int radius = 7;
		float jitter = 0.8F;
		float scale = 0.25F;
		Noise densityVariance = Noises.simplex(12345 + 1, 150, 1);
		densityVariance = Noises.mul(densityVariance, 0.75F);
		densityVariance = Noises.add(densityVariance, 1.0F - 0.75F);
		FastPoissonContext context = new FastPoissonContext(radius, jitter, scale, new DensityNoise(etcodehome.freeterraforged.world.worldgen.feature.placement.poisson.BiomeVariance.NONE, densityVariance));
		int totalPoints = 0;
		for (int cx = 0; cx < 10; cx++) {
			for (int cz = 0; cz < 10; cz++) {
				List<BlockPos> points = new ArrayList<>();
				poisson.visit(12345, cx, cz, new Random(12345 + cx * 31 + cz), context, points, (x, z, list) -> {
					list.add(new BlockPos(x, 0, z));
				});
				totalPoints += points.size();
			}
		}
		System.out.println("FastPoisson total points for 100 chunks: " + totalPoints + " (avg: " + (totalPoints / 100.0) + " per chunk)");
	}

	@Test
	void testLoadAll101Templates() throws Exception {
		java.nio.file.Path root = java.nio.file.Paths.get("src/main/resources/data/freeterraforged/structures");
		if (!java.nio.file.Files.exists(root)) {
			root = java.nio.file.Paths.get("common/src/main/resources/data/freeterraforged/structures");
		}
		assertTrue(java.nio.file.Files.exists(root), "structures root directory should exist");

		List<java.nio.file.Path> nbtFiles;
		try (var stream = java.nio.file.Files.walk(root)) {
			nbtFiles = stream.filter(p -> p.toString().endsWith(".nbt")).toList();
		}

		assertEquals(101, nbtFiles.size(), "Should have exactly 101 NBT template files");

		int passed = 0;
		for (java.nio.file.Path nbtPath : nbtFiles) {
			try (InputStream is = java.nio.file.Files.newInputStream(nbtPath)) {
				Optional<FeatureTemplate> opt = FeatureTemplate.load(is);
				assertTrue(opt.isPresent(), "Failed to load " + nbtPath);
				FeatureTemplate t = opt.get();
				Dimensions dim = t.getDimensions(Mirror.NONE, Rotation.NONE);
				assertTrue(dim.getSizeX() > 0, "Width should be > 0 for " + nbtPath);
				assertTrue(dim.getSizeY() > 0, "Height should be > 0 for " + nbtPath);
				assertTrue(dim.getSizeZ() > 0, "Depth should be > 0 for " + nbtPath);
				passed++;
			}
		}
		System.out.println("Successfully validated all " + passed + " template NBT files!");
	}

	@Test
	void testLoadForestOak1() throws Exception {
		InputStream stream = getClass().getResourceAsStream("/data/freeterraforged/structures/trees/oak/forest/forest_oak_1.nbt");
		assertNotNull(stream, "forest_oak_1.nbt stream should not be null");

		Optional<FeatureTemplate> templateOpt = FeatureTemplate.load(stream);
		assertTrue(templateOpt.isPresent(), "FeatureTemplate should load successfully");

		FeatureTemplate template = templateOpt.get();
		Dimensions dim = template.getDimensions(Mirror.NONE, Rotation.NONE);
		System.out.println("Template dimensions: min=" + dim.min() + ", max=" + dim.max() + ", size=(" + dim.getSizeX() + "," + dim.getSizeY() + "," + dim.getSizeZ() + ")");

		Field templateField = FeatureTemplate.class.getDeclaredField("template");
		templateField.setAccessible(true);
		BakedTemplate baked = (BakedTemplate) templateField.get(template);
		BlockInfo[] blocks = baked.get(Mirror.NONE, Rotation.NONE);

		System.out.println("Total blocks in template: " + blocks.length);
		int airCount = 0;
		int logCount = 0;
		int leavesCount = 0;
		int otherCount = 0;

		for (BlockInfo b : blocks) {
			if (b.state().isAir()) {
				airCount++;
			} else if (b.state().is(Blocks.OAK_LOG)) {
				logCount++;
			} else if (b.state().is(Blocks.OAK_LEAVES)) {
				leavesCount++;
			} else {
				otherCount++;
			}
		}

		System.out.println("Counts: logs=" + logCount + ", leaves=" + leavesCount + ", air=" + airCount + ", other=" + otherCount);
		assertNotEquals(0, logCount, "Logs count should not be 0!");
		assertNotEquals(0, leavesCount, "Leaves count should not be 0!");
	}

	@Test
	void testPasteWorldGen() throws Exception {
		InputStream stream = getClass().getResourceAsStream("/data/freeterraforged/structures/trees/oak/forest/forest_oak_1.nbt");
		Optional<FeatureTemplate> templateOpt = FeatureTemplate.load(stream);
		assertTrue(templateOpt.isPresent());
		FeatureTemplate template = templateOpt.get();

		Map<BlockPos, BlockState> worldBlocks = new HashMap<>();
		int groundY = 64;

		WorldGenLevel mockLevel = (WorldGenLevel) Proxy.newProxyInstance(
			WorldGenLevel.class.getClassLoader(),
			new Class<?>[]{WorldGenLevel.class},
			(proxy, method, args) -> {
				String name = method.getName();
				if (name.equals("getBlockState")) {
					BlockPos pos = (BlockPos) args[0];
					BlockState state = worldBlocks.get(pos);
					if (state != null) return state;
					if (pos.getY() < groundY) return Blocks.DIRT.defaultBlockState();
					if (pos.getY() == groundY) return Blocks.GRASS_BLOCK.defaultBlockState();
					return Blocks.AIR.defaultBlockState();
				}
				if (name.equals("setBlock")) {
					BlockPos pos = (BlockPos) args[0];
					BlockState state = (BlockState) args[1];
					worldBlocks.put(pos.immutable(), state);
					return true;
				}
				if (name.equals("isStateAtPosition")) {
					BlockPos pos = (BlockPos) args[0];
					java.util.function.Predicate<BlockState> predicate = (java.util.function.Predicate<BlockState>) args[1];
					BlockState state = worldBlocks.get(pos);
					if (state == null) {
						if (pos.getY() < groundY) state = Blocks.DIRT.defaultBlockState();
						else if (pos.getY() == groundY) state = Blocks.GRASS_BLOCK.defaultBlockState();
						else state = Blocks.AIR.defaultBlockState();
					}
					return predicate.test(state);
				}
				if (name.equals("getMaxY")) {
					return 320;
				}
				if (name.equals("getMinY")) {
					return -64;
				}
				if (name.equals("getHeight")) {
					return 384;
				}
				return null;
			}
		);

		BlockPos origin = new BlockPos(8, groundY + 1, 8);
		Dimensions dim = template.getDimensions(Mirror.NONE, Rotation.NONE);
		TemplatePlacement<TreeContext> placement = TemplatePlacements.tree();

		boolean isSoil = etcodehome.freeterraforged.world.worldgen.feature.template.BlockUtils.isSoil(mockLevel, origin.below());
		boolean isClear = etcodehome.freeterraforged.world.worldgen.feature.template.BlockUtils.isClearOverhead(mockLevel, origin, dim.getSizeY(), etcodehome.freeterraforged.world.worldgen.feature.template.BlockUtils::canTreeReplace);
		System.out.println("isSoil: " + isSoil + ", isClear: " + isClear + ", origin.below() state=" + mockLevel.getBlockState(origin.below()));

		boolean canPlace = placement.canPlaceAt(mockLevel, origin, dim);
		System.out.println("canPlaceAt: " + canPlace);
		assertTrue(canPlace, "canPlaceAt should be true at groundY + 1 on grass");

		TreeContext ctx = placement.createContext();
		PasteConfig pasteConfig = new PasteConfig(3, false, false, false, false);
		boolean pasted = template.pasteWorldGen(mockLevel, ctx, origin, Mirror.NONE, Rotation.NONE, placement, pasteConfig);
		System.out.println("pasted: " + pasted + ", placed blocks in world: " + worldBlocks.size());
		assertTrue(pasted, "pasteWorldGen should succeed");
		assertTrue(worldBlocks.size() > 50, "Should have placed many blocks");
	}
}
