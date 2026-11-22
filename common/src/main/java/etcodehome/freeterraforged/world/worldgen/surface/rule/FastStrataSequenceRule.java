package etcodehome.freeterraforged.world.worldgen.surface.rule;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import com.mojang.serialization.MapCodec;
import org.jetbrains.annotations.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.material.MaterialRuleContext;
import net.minecraft.world.level.levelgen.material.MaterialRules;
import net.minecraft.world.level.levelgen.material.rule.MaterialRule;
import net.minecraft.world.level.levelgen.material.rule.RuleEvaluator;
import etcodehome.freeterraforged.world.worldgen.perf.WorldgenPerfTracker;
import etcodehome.freeterraforged.world.worldgen.surface.ISurfaceContext;

public class FastStrataSequenceRule implements RuleEvaluator {
	private final MaterialRuleContext context;
	private final RuleEvaluator base;
	private final @Nullable RuleEvaluator strata;
	private final int minBedrockY;
	private long lastCheckedXZ = Long.MIN_VALUE;
	private boolean isBadlands;
	private boolean isDesert;

	public FastStrataSequenceRule(
			MaterialRuleContext context,
			RuleEvaluator base,
			@Nullable RuleEvaluator strata
	) {
		this.context = context;
		this.base = base;
		this.strata = strata;
		this.minBedrockY = (context.context != null ? context.context.getMinGenY() : -64) + 5;
	}

	@Override
	public BlockState tryApply(int x, int y, int z) {
		if (this.lastCheckedXZ != this.context.lastUpdateXZ) {
			this.lastCheckedXZ = this.context.lastUpdateXZ;
			Holder<Biome> biomeHolder = this.context.biome != null ? this.context.biome : this.context.getBiome();
			this.isBadlands = biomeHolder != null && biomeHolder.is(BiomeTags.IS_BADLANDS);
			this.isDesert = biomeHolder != null && (biomeHolder.is(Biomes.DESERT)
					|| biomeHolder.unwrapKey().map(k -> k.identifier().getPath().contains("desert")).orElse(false));
		}

		if (!this.isBadlands) {
			int depthAbove = ((Object) this.context) instanceof ISurfaceContext sc
				? sc.freeterraforged$getStoneDepthAbove()
				: this.context.stoneDepthAbove();
			int depthBelow = ((Object) this.context) instanceof ISurfaceContext sc
				? sc.freeterraforged$getStoneDepthBelow()
				: this.context.stoneDepthBelow();

			if (y <= 0 && y > this.minBedrockY && depthAbove > 10 && (depthBelow <= 0 || depthBelow > 4)) {
				if (WorldgenPerfTracker.ACTIVE) {
					WorldgenPerfTracker.stats().strataFastPathHits++;
				}
				if (this.strata != null) {
					BlockState strataState = this.strata.tryApply(x, y, z);
					if (strataState != null) {
						return strataState;
					}
				}
				return Blocks.DEEPSLATE.defaultBlockState();
			}

			int minDepthAbove = 20;
			if (y > 8 && depthAbove > minDepthAbove && (depthBelow <= 0 || depthBelow > 4)) {
				if (WorldgenPerfTracker.ACTIVE) {
					WorldgenPerfTracker.stats().strataFastPathHits++;
				}
				return this.strata != null ? this.strata.tryApply(x, y, z) : null;
			}
		}

		if (WorldgenPerfTracker.ACTIVE) {
			WorldgenPerfTracker.stats().strataBaseCalls++;
		}
		BlockState state = this.base.tryApply(x, y, z);
		if (state != null) {
			return state;
		}
		return this.strata != null ? this.strata.tryApply(x, y, z) : null;
	}

	public static MaterialRule optimize(MaterialRule root) {
		if (root == null || root instanceof Source) {
			return root;
		}
		StrataRule strata = findStrataRule(root);
		MaterialRule base = strata != null ? removeStrataRule(root, strata) : root;
		return new Source(base != null ? base : MaterialRules.sequence(), strata);
	}

	public static StrataRule findStrataRule(MaterialRule source) {
		if (source == null) {
			return null;
		}
		if (source instanceof StrataRule strata) {
			return strata;
		}
		if (source instanceof AcquiredSurfaceRule acquired) {
			return findStrataRule(acquired.root());
		}
		List<MaterialRule> children = getChildren(source);
		if (children != null) {
			for (int i = children.size() - 1; i >= 0; i--) {
				StrataRule found = findStrataRule(children.get(i));
				if (found != null) {
					return found;
				}
			}
		}
		return null;
	}

	public static MaterialRule removeStrataRule(MaterialRule source, StrataRule target) {
		if (source == null || source == target || source.equals(target)) {
			return null;
		}
		if (source instanceof StrataRule) {
			return source;
		}
		if (source instanceof AcquiredSurfaceRule acquired) {
			MaterialRule inner = removeStrataRule(acquired.root(), target);
			return inner != null ? new AcquiredSurfaceRule(inner, acquired.sources(), acquired.failures()) : null;
		}
		List<MaterialRule> children = getChildren(source);
		if (children != null) {
			List<MaterialRule> filtered = new ArrayList<>(children.size());
			boolean changed = false;
			for (MaterialRule child : children) {
				MaterialRule updated = removeStrataRule(child, target);
				if (updated != child) {
					changed = true;
				}
				if (updated != null) {
					filtered.add(updated);
				}
			}
			if (changed) {
				if (filtered.isEmpty()) {
					return null;
				}
				if (filtered.size() == 1) {
					return filtered.get(0);
				}
				return MaterialRules.sequence(filtered.toArray(new MaterialRule[0]));
			}
		}
		return source;
	}

	@SuppressWarnings("unchecked")
	public static List<MaterialRule> getChildren(MaterialRule source) {
		if (source == null || source instanceof StrataRule) {
			return null;
		}
		Class<?> clazz = source.getClass();
		if (clazz.isRecord()) {
			for (RecordComponent rc : clazz.getRecordComponents()) {
				if (List.class.isAssignableFrom(rc.getType())) {
					try {
						Method accessor = rc.getAccessor();
						accessor.setAccessible(true);
						Object val = accessor.invoke(source);
						if (val instanceof List<?> l && isRuleSourceList(rc.getGenericType(), l)) {
							return (List<MaterialRule>) l;
						}
					} catch (Throwable ignored) {}
				}
			}
		}
		try {
			Method m = clazz.getMethod("sequence");
			m.setAccessible(true);
			Object val = m.invoke(source);
			if (val instanceof List<?> l && isRuleSourceList(m.getGenericReturnType(), l)) {
				return (List<MaterialRule>) l;
			}
		} catch (Throwable ignored) {}
		for (Field f : clazz.getDeclaredFields()) {
			if (List.class.isAssignableFrom(f.getType())) {
				try {
					f.setAccessible(true);
					Object val = f.get(source);
					if (val instanceof List<?> l && isRuleSourceList(f.getGenericType(), l)) {
						return (List<MaterialRule>) l;
					}
				} catch (Throwable ignored) {}
			}
		}
		return null;
	}

	private static boolean isRuleSourceList(Type genericType, List<?> list) {
		if (genericType instanceof ParameterizedType pt) {
			Type[] args = pt.getActualTypeArguments();
			if (args.length == 1 && args[0] instanceof Class<?> elemClass) {
				return MaterialRule.class.isAssignableFrom(elemClass);
			}
		}
		if (list.isEmpty()) {
			return false;
		}
		for (Object elem : list) {
			if (!(elem instanceof MaterialRule)) {
				return false;
			}
		}
		return true;
	}

	public record Source(MaterialRule base, @Nullable StrataRule strata) implements MaterialRule {
		@Override
		public RuleEvaluator compile(MaterialRuleContext context) {
			RuleEvaluator baseRule = this.base.compile(context);
			RuleEvaluator strataRule = this.strata != null ? this.strata.compile(context) : null;
			return new FastStrataSequenceRule(context, baseRule, strataRule);
		}

		@Override
		public MapCodec<? extends MaterialRule> codec() {
			return this.base.codec();
		}
	}
}
