package etcodehome.freeterraforged.mixin;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup.RegistryLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunctions;
import net.minecraft.world.level.levelgen.densityfunction.DensitySampler;
import net.minecraft.world.level.levelgen.densityfunction.DfRewriteRule;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

import etcodehome.freeterraforged.FTFCommon;
import etcodehome.freeterraforged.config.PerformanceConfig;
import etcodehome.freeterraforged.data.worldgen.preset.PresetManager;
import etcodehome.freeterraforged.data.worldgen.preset.settings.Preset;
import etcodehome.freeterraforged.registries.FTFRegistries;
import etcodehome.freeterraforged.tags.FTFDensityFunctionTags;
import etcodehome.freeterraforged.world.worldgen.GeneratorContext;
import etcodehome.freeterraforged.world.worldgen.FTFRandomState;
import etcodehome.freeterraforged.world.worldgen.biome.ClimateQuerySemantics;
import etcodehome.freeterraforged.world.worldgen.biome.FTFClimateSampler;
import etcodehome.freeterraforged.world.worldgen.densityfunction.CellSampler;
import etcodehome.freeterraforged.world.worldgen.densityfunction.ClimateDepthFunction;
import etcodehome.freeterraforged.world.worldgen.densityfunction.NoiseFunction;
import etcodehome.freeterraforged.world.worldgen.noise.module.Noise;
import etcodehome.freeterraforged.world.worldgen.noise.module.Noises;
import etcodehome.freeterraforged.world.worldgen.runtime.TerraForgedChunkGenerator;
import etcodehome.freeterraforged.world.worldgen.runtime.WorldgenEpoch;
import etcodehome.freeterraforged.world.worldgen.runtime.WorldgenPlan;
import etcodehome.freeterraforged.world.worldgen.runtime.WorldgenPlans;
import etcodehome.freeterraforged.world.worldgen.runtime.WorldgenRuntimeBinding;
import etcodehome.freeterraforged.world.worldgen.util.Seed;

@Mixin(RandomState.class)
@Implements(@Interface(iface = FTFRandomState.class, prefix = "freeterraforged$FTFRandomState$"))
class MixinRandomState {

	@Shadow @Final private long seed;
	@Shadow @Final private HolderGetter<NormalNoise> noises;
	@Unique @Nullable private Climate.Sampler sampler;

	private DfRewriteRule densityFunctionWrapper;
	private boolean hasContext;
	@Unique private boolean freeterraforged$isFTFDimension = false;
	@Unique private final Map<IdentityKey<DensityFunction>, DensityFunction> freeterraforged$rewrittenFunctions = new ConcurrentHashMap<>();
	@Unique private final Map<IdentityKey<DensityFunction>, DensitySampler> freeterraforged$compiledSamplers = new ConcurrentHashMap<>();
	@Nullable private volatile ClimateQuerySemantics freeterraforged$sharedClimateSemantics;
	@Nullable private volatile GeneratorContext generatorContext;
	@Nullable private volatile Preset preset;
	@Nullable private volatile WorldgenEpoch worldgenEpoch;
	@Nullable private volatile WorldgenRuntimeBinding worldgenBinding;

	private static final class IdentityKey<T> {
		private final T value;
		private final int hash;

		IdentityKey(T value) {
			this.value = value;
			this.hash = System.identityHashCode(value);
		}

		@Override
		public int hashCode() {
			return this.hash;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj instanceof IdentityKey<?> other) {
				return this.value == other.value;
			}
			return false;
		}
	}

	@Unique
	private DfRewriteRule freeterraforged$getOrCreateWrapper() {
		if (this.densityFunctionWrapper == null) {
			this.densityFunctionWrapper = this.freeterraforged$createDensityFunctionWrapper(this.seed, this.noises);
		}
		return this.densityFunctionWrapper;
	}

	@ModifyVariable(
		method = "<init>",
		at = @At(
			value = "FIELD",
			target = "Lnet/minecraft/world/level/levelgen/RandomState;noises:Lnet/minecraft/core/HolderGetter;",
			shift = At.Shift.AFTER
		),
		argsOnly = true
	)
	private NoiseRouter freeterraforged$modifyRouter(NoiseRouter router) {
		return freeterraforged$wrapNoiseRouter(router, this.freeterraforged$getOrCreateWrapper());
	}

	@Inject(
		method = "createClimateSampler",
		at = @At("RETURN")
	)
	private void freeterraforged$onCreateClimateSampler(SamplerContext context, CallbackInfoReturnable<Climate.Sampler> cir) {
		Climate.Sampler created = cir.getReturnValue();
		if (this.sampler == null) {
			this.sampler = created;
		}
		this.freeterraforged$bindClimateSampler(created);
	}

	@Unique
	private void freeterraforged$bindClimateSampler(Climate.Sampler targetSampler) {
		if (targetSampler == null) {
			return;
		}
		if ((Object) targetSampler instanceof FTFClimateSampler ftfSampler) {
			if (this.worldgenBinding != null) {
				ftfSampler.setWorldgenBinding(this.worldgenBinding);
				WorldgenPlan plan = this.worldgenBinding.plan();
				Preset currentPreset = this.preset;
				GeneratorContext currentContext = this.generatorContext;
				if (currentPreset != null && currentContext != null) {
					ClimateQuerySemantics shared = this.freeterraforged$sharedClimateSemantics;
					if (shared != null
						&& shared.policy() == plan.samplerDecoration().queryPolicy()
						&& shared.preset() == currentPreset
						&& shared.seed() == plan.owner().seed()
						&& shared.surfaceContext() == currentContext) {
						ftfSampler.adoptClimateQuerySemantics(shared);
					} else {
						plan.samplerDecoration().initialize(
							plan,
							new WorldgenPlans.SamplerInputs(currentPreset, currentContext),
							targetSampler
						);
						this.freeterraforged$sharedClimateSemantics = ftfSampler.climateQuerySemantics();
					}
				}
			}
		}
	}

	@Unique
	private DfRewriteRule freeterraforged$createDensityFunctionWrapper(long seed, HolderGetter<NormalNoise> noises) {
		return new DfRewriteRule() {
			private final Map<CellSampler.Field, CellSampler> cells = new ConcurrentHashMap<>();

			@Override
			public DensityFunction rewrite(DensityFunction function) {
				if (function == null) {
					return null;
				}
				IdentityKey<DensityFunction> key = new IdentityKey<>(function);
				DensityFunction cached = MixinRandomState.this.freeterraforged$rewrittenFunctions.get(key);
				if (cached != null) {
					return cached;
				}

				if (function instanceof DensityFunctions.HolderHolder holder) {
					DensityFunction inlined = this.rewrite(holder.function().value());
					MixinRandomState.this.freeterraforged$rewrittenFunctions.put(key, inlined);
					return inlined;
				}

				if (function instanceof NoiseFunction.Marker marker) {
					DensityFunction rewritten = new NoiseFunction(marker.noise(), Seed.toInt(seed));
					MixinRandomState.this.freeterraforged$rewrittenFunctions.put(key, rewritten);
					return rewritten;
				}

				if (function instanceof ClimateDepthFunction.Marker marker) {
					MixinRandomState.this.hasContext = true;
					DensityFunction rewritten = new ClimateDepthFunction(
						() -> {
							GeneratorContext ctx = MixinRandomState.this.generatorContext;
							return ctx != null ? ctx.lookup : null;
						},
						marker.worldDepth(),
						marker.worldHeight()
					);
					MixinRandomState.this.freeterraforged$rewrittenFunctions.put(key, rewritten);
					return rewritten;
				}

				if (function instanceof CellSampler.Marker marker) {
					MixinRandomState.this.hasContext = true;
					DensityFunction rewritten = this.cells.computeIfAbsent(marker.field(), field -> new CellSampler(
						() -> {
							GeneratorContext ctx = MixinRandomState.this.generatorContext;
							return ctx != null ? ctx.lookup : null;
						},
						field,
						freeterraforged$createCellFallback(field, this, noises)
					));
					MixinRandomState.this.freeterraforged$rewrittenFunctions.put(key, rewritten);
					return rewritten;
				}

				DensityFunction rewritten = function.rewriteChildren(this);
				MixinRandomState.this.freeterraforged$rewrittenFunctions.put(key, rewritten);
				return rewritten;
			}
		};
	}

	@Unique
	private static NoiseRouter freeterraforged$wrapNoiseRouter(NoiseRouter router, DfRewriteRule visitor) {
		return new NoiseRouter(
			visitor.rewrite(router.temperature()),
			visitor.rewrite(router.vegetation()),
			visitor.rewrite(router.continents()),
			visitor.rewrite(router.erosion()),
			visitor.rewrite(router.depth()),
			visitor.rewrite(router.ridges()),
			visitor.rewrite(router.chunkSurfaceLevel()),
			visitor.rewrite(router.finalDensity())
		);
	}

	public synchronized void freeterraforged$FTFRandomState$initialize(WorldgenEpoch epoch) {
		if (this.worldgenEpoch != null) {
			if (this.worldgenEpoch.id().equals(epoch.id())) {
				return;
			}
			throw new IllegalStateException("RandomState is already owned by worldgen epoch " + this.worldgenEpoch.id());
		}
		RegistryAccess registries = epoch.registries();
		RegistryLookup<Preset> presets = registries.lookupOrThrow(FTFRegistries.PRESET);
		if (epoch.selectedStem().generator() instanceof TerraForgedChunkGenerator || this.hasContext) {
			this.freeterraforged$isFTFDimension = true;
		}
		Preset initializedPreset = presets.get(Preset.KEY)
			.map((presetHolder) -> presetHolder.value())
			.orElse(null);

		GeneratorContext initializedContext = null;
		try {
			if (this.freeterraforged$isFTFDimension) {
				if (initializedPreset == null) {
					throw new IllegalStateException("RTF density graph is active but the selected preset is unavailable");
				}
				PresetManager.PM.ingestFromPreset(initializedPreset);
				PresetManager.PM.loadCheck();
				RegistryLookup<Noise> noisesLookup = registries.lookupOrThrow(FTFRegistries.NOISE);
				RegistryLookup<DensityFunction> functions = registries.lookupOrThrow(Registries.DENSITY_FUNCTION);

				functions.get(FTFDensityFunctionTags.ADDITIONAL_NOISE_ROUTER_FUNCTIONS).ifPresent((set) -> {
					DfRewriteRule wrapper = this.freeterraforged$getOrCreateWrapper();
					set.forEach((function) -> wrapper.rewrite(function.value()));
				});

				PerformanceConfig config = PerformanceConfig.read(PerformanceConfig.DEFAULT_FILE_PATH)
						.resultOrPartial(FTFCommon.LOGGER::error)
						.orElseGet(PerformanceConfig::makeDefault);
				initializedContext = GeneratorContext.make(initializedPreset, noisesLookup, this.seed);
			}
			this.preset = initializedPreset;
			this.generatorContext = initializedContext;
			this.freeterraforged$sharedClimateSemantics = null;
			this.worldgenEpoch = epoch;
			if (this.sampler != null) {
				this.freeterraforged$bindClimateSampler(this.sampler);
			}
		} catch (RuntimeException | Error failure) {
			if (initializedContext != null) {
				try {
					initializedContext.close();
				} catch (RuntimeException | Error cleanupFailure) {
					failure.addSuppressed(cleanupFailure);
				}
			}
			this.preset = null;
			this.generatorContext = null;
			this.freeterraforged$sharedClimateSemantics = null;
			throw failure;
		}
	}

	public synchronized void freeterraforged$FTFRandomState$bindPlan(WorldgenRuntimeBinding binding) {
		WorldgenPlan plan = binding.plan();
		if (this.worldgenEpoch == null || !this.worldgenEpoch.id().equals(plan.owner().id())) {
			throw new IllegalStateException("Cannot bind a plan owned by a different worldgen epoch");
		}
		if (this.worldgenBinding != null && this.worldgenBinding != binding) {
			throw new IllegalStateException("RandomState already has a compiled worldgen plan");
		}
		this.worldgenBinding = binding;
		this.freeterraforged$sharedClimateSemantics = null;
		if (this.sampler != null) {
			this.freeterraforged$bindClimateSampler(this.sampler);
		}
	}

	public synchronized void freeterraforged$FTFRandomState$preparePlanRebind(WorldgenEpoch epoch, WorldgenPlan plan) {
		WorldgenRuntimeBinding binding = this.worldgenBinding;
		if (binding == null || !binding.epoch().id().equals(epoch.id())) {
			throw new IllegalStateException("Cannot refresh a different worldgen epoch");
		}
		if (!plan.owner().id().equals(epoch.id())) {
			throw new IllegalStateException("Refreshed plan is owned by a different worldgen epoch");
		}
		WorldgenEpoch current = binding.epoch();
		if (!epoch.inputRevisionStrictlyAdvances(current)) {
			throw new IllegalStateException("A plan rebind must advance a worldgen input epoch");
		}
		if (epoch.inputRevisionRegressesFrom(current)) {
			throw new IllegalStateException("Worldgen input epochs must advance monotonically");
		}
		WorldgenPlans.SamplerDecoration currentSampler = binding.plan().samplerDecoration();
		WorldgenPlans.SamplerDecoration replacementSampler = plan.samplerDecoration();
		if (currentSampler.queryPolicy() != replacementSampler.queryPolicy()) {
			throw new IllegalStateException(
				"A sampler query-policy change requires a new worldgen owner; it cannot be published by reload"
			);
		}
	}

	@Nullable
	public WorldgenEpoch freeterraforged$FTFRandomState$epoch() {
		WorldgenRuntimeBinding binding = this.worldgenBinding;
		return binding == null ? this.worldgenEpoch : binding.epoch();
	}

	@Nullable
	public WorldgenPlan freeterraforged$FTFRandomState$plan() {
		WorldgenRuntimeBinding binding = this.worldgenBinding;
		return binding == null ? null : binding.plan();
	}

	@Nullable
	public WorldgenRuntimeBinding freeterraforged$FTFRandomState$binding() {
		return this.worldgenBinding;
	}

	public boolean freeterraforged$FTFRandomState$isTerraForged() {
		return this.freeterraforged$isFTFDimension;
	}

	@Nullable
	public Preset freeterraforged$FTFRandomState$preset() {
		return this.preset;
	}

	@Nullable
	public GeneratorContext freeterraforged$FTFRandomState$generatorContext() {
		return this.generatorContext;
	}

	public Noise freeterraforged$FTFRandomState$seed(Noise noise) {
		return Noises.shiftSeed(noise, Seed.toInt(this.seed));
	}

	public long freeterraforged$FTFRandomState$seed() {
		return this.seed;
	}

	public synchronized void freeterraforged$FTFRandomState$close() {
		GeneratorContext closing = this.generatorContext;
		this.generatorContext = null;
		this.worldgenBinding = null;
		this.worldgenEpoch = null;
		this.freeterraforged$isFTFDimension = false;
		this.preset = null;
		this.sampler = null;
		this.freeterraforged$sharedClimateSemantics = null;
		this.freeterraforged$compiledSamplers.clear();
		this.freeterraforged$rewrittenFunctions.clear();
		this.densityFunctionWrapper = null;
		if (closing != null) {
			closing.close();
		}
	}

	@Nullable
	@Unique
	private static DensityFunction freeterraforged$createCellFallback(
		CellSampler.Field field,
		DfRewriteRule visitor,
		HolderGetter<NormalNoise> params
	) {
		if (params == null) {
			return DensityFunctions.zero();
		}
		var shiftOpt = params.get(net.minecraft.world.level.levelgen.Noises.SHIFT);
		if (shiftOpt.isEmpty()) {
			return DensityFunctions.zero();
		}
		var shift = shiftOpt.orElseThrow();
		DensityFunction shiftX = DensityFunctions.shiftA(shift);
		DensityFunction shiftZ = DensityFunctions.shiftB(shift);
		return switch (field) {
			case CONTINENT -> params.get(net.minecraft.world.level.levelgen.Noises.CONTINENTALNESS)
				.map(noise -> visitor.rewrite(DensityFunctions.shiftedNoise2d(shiftX, shiftZ, 0.25D, noise)))
				.orElseGet(DensityFunctions::zero);
			case EROSION, TERRAIN_EROSION, HEIGHT_EROSION -> params.get(net.minecraft.world.level.levelgen.Noises.EROSION)
				.map(noise -> visitor.rewrite(DensityFunctions.shiftedNoise2d(shiftX, shiftZ, 0.25D, noise)))
				.orElseGet(DensityFunctions::zero);
			case WEIRDNESS -> params.get(net.minecraft.world.level.levelgen.Noises.RIDGE)
				.map(noise -> visitor.rewrite(DensityFunctions.shiftedNoise2d(shiftX, shiftZ, 0.25D, noise)))
				.orElseGet(DensityFunctions::zero);
			case TEMPERATURE -> params.get(net.minecraft.world.level.levelgen.Noises.TEMPERATURE)
				.map(noise -> visitor.rewrite(DensityFunctions.shiftedNoise2d(shiftX, shiftZ, 0.25D, noise)))
				.orElseGet(DensityFunctions::zero);
			case MOISTURE -> params.get(net.minecraft.world.level.levelgen.Noises.VEGETATION)
				.map(noise -> visitor.rewrite(DensityFunctions.shiftedNoise2d(shiftX, shiftZ, 0.25D, noise)))
				.orElseGet(DensityFunctions::zero);
			default -> DensityFunctions.zero();
		};
	}

	@Unique
	private DensityFunction freeterraforged$rewriteDensityFunction(DensityFunction function) {
		if (function == null) {
			return null;
		}
		IdentityKey<DensityFunction> key = new IdentityKey<>(function);
		DensityFunction cached = this.freeterraforged$rewrittenFunctions.get(key);
		if (cached != null) {
			return cached;
		}
		DfRewriteRule wrapper = this.freeterraforged$getOrCreateWrapper();
		DensityFunction rewritten = wrapper.rewrite(function);
		this.freeterraforged$rewrittenFunctions.put(key, rewritten);
		return rewritten;
	}

	@WrapMethod(method = "getSampler")
	private DensitySampler freeterraforged$wrapGetSampler(
		DensityFunction function,
		Operation<DensitySampler> original
	) {
		if (function == null) {
			return original.call((Object) null);
		}
		IdentityKey<DensityFunction> key = new IdentityKey<>(function);
		DensitySampler cached = this.freeterraforged$compiledSamplers.get(key);
		if (cached != null) {
			return cached;
		}
		DensityFunction rewritten = this.freeterraforged$rewriteDensityFunction(function);
		DensitySampler compiled = original.call(rewritten);
		this.freeterraforged$compiledSamplers.put(key, compiled);
		return compiled;
	}

	public DensityFunction freeterraforged$FTFRandomState$wrap(DensityFunction function) {
		return this.freeterraforged$rewriteDensityFunction(function);
	}
}
