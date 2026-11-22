# FreeTerraForged — Minecraft 26.3 (*Wilderness Bound*) Porting Status

**Branch:** `26.3`  
**Target:** Minecraft `26.3` | Java `25` | Gradle `9.7.1` | Fabric Loom `1.18.4` | Fabric Loader `0.19.5`  
**Status:** **PORT COMPLETE & FULLY VERIFIED**  
- **Compilation:** **0 Errors** across all modules (`:common` [262 files], `:common` tests [5 files], and `:fabric` [15 files])
- **Unit Tests:** **35 / 35 Passed** (`BUILD SUCCESSFUL`, 0 failures, 0 errors, 0 skipped)
- **Bytecode & Mixin Audit:** **43 / 43 Mixins Verified** against Minecraft 26.3 merged jar bytecode (0 descriptor / shadow errors)
- **Runtime Server Launch & Preset Validation:** **20 / 20 Validation Tests Passed** on headless Fabric Loader 26.3 server (exited code 0)
- **Native 26.3 Mod Compatibility Bridges:** **TerraBlender 26.3**, **Biolith 26.3**, and **Lithostitched 26.3** native API integration verified with 0 reflection workarounds.

---

## 1. Summary of Completed Porting Work

### 1.1 Toolchain, Build System & Access Widener
- **Gradle & Loom:** Updated [`gradle-wrapper.properties`](gradle/wrapper/gradle-wrapper.properties) to **Gradle 9.7.1** and configured **`net.fabricmc.fabric-loom` `1.18.4`** targeting **Java 25**.
- **Access Widener:** Rewrote [`freeterraforged.accesswidener`](common/src/main/resources/freeterraforged.accesswidener) (`v2 official`) with Minecraft 26.3 field, method, and class targets (including `RegistrySynchronization.NETWORKABLE_REGISTRIES`, `MaterialRuleContext`, `NoiseRouterData`, `Climate$RTree`, `CreateWorldScreen`, etc.). Configured `"accessWidener": "freeterraforged.accesswidener"` in [`fabric.mod.json`](fabric/src/main/resources/fabric.mod.json).
- **Workspace Hygiene:** Removed stray untracked directories (`local-repo/` and `mappings/`).

### 1.2 Common Engine (`:common` — 262 Java source files, 0 errors)
- **Density Functions & Noise:** Migrated to Minecraft 26.3's compiled `DensityFunction` / `DensitySampler` / `DensityVolume` / `DfRewriteRule` architecture (`float` domain, `Interval` bounds, `domainAxes()`, `compileSampler(CompileContext)`).
- **Surface / Material Rules:** Replaced 1.21.1 `SurfaceRules.RuleSource` with 26.3's registry-backed `MaterialRule` / `MaterialEvaluator` / `MaterialRuleContext` / `MaterialSystem`.
- **Worldgen Runtime & Lifecycle:** Updated `TerraForgedChunkGenerator` to unified `ChunkGenerator.buildTerrain(...)`, implemented 26.3's `BiomeSource.createResolver(Climate.Sampler)`, and updated `WorldgenPlans`, `SurfaceColumnPlan`, and `SurfaceRuleComposition` for modular material rules and flattened `WorldCarver`.
- **Features & Placement Modifiers:** Adapted all custom features to 26.3's unified `Feature<C extends FeatureConfiguration>` hierarchy (`DiskFeature`, `DecorateSnowFeature`, `ScreeFeature`, `SwampSurfaceFeature`, `BushFeature`, `TemplateFeature`, etc.) and placement modifier classes.
- **Preset Datapack Generation:** Updated `PresetNoiseGeneratorSettings`, `PresetNoiseRouterData`, `PresetDimensionTypes`, `PresetPlacedFeatures`, and `FTFWorldPresetDataPacks` for 26.3 codecs and records.
- **Client GUI & Rendering:** Ported screen and widget classes (`PresetConfigScreen`, `PresetListPage`, `Preview2D`, `Preview3D`, `FlowFieldDebugRenderer`) to 26.3's `GuiGraphicsExtractor`, `MouseButtonEvent`, and modern vertex buffers.
- **Validation Runner:** Updated [`PresetValidationRunner.java`](common/src/main/java/etcodehome/freeterraforged/test/PresetValidationRunner.java) for 26.3 candidate cave biomes (`sulfur_caves`), `overworldEntries.values()`, and added non-blocking guard for diagnostic histogram execution.

### 1.3 `SpawnFinderFix` Radial Search Restoration
- **Radial Search Refinement:** Updated [`SpawnFinderFix.java`](common/src/main/java/etcodehome/freeterraforged/world/worldgen/cell/biome/spawn/SpawnFinderFix.java) to provide direct `SpawnFinderFix(BlockPos center, List<ParameterPoint> list, Sampler sampler)` constructor.
- **Generator Origin Refinement:** Updated [`TerraForgedChunkGenerator.getOrigin(RandomState)`](common/src/main/java/etcodehome/freeterraforged/world/worldgen/runtime/TerraForgedChunkGenerator.java): when `spawnType == SpawnType.CONTINENT_CENTER` with non-empty parameter points, creates cached `Climate.Sampler` via `randomState.createClimateSampler(SamplerContext.builder().enableCaches().build())`, executes `SpawnFinderFix` outward radial search around the continent center to locate moderate inland continentalness (`[-0.11, 0.55]`), and returns `ChunkPos.containing(refined)`.
- **Vanilla Generator Mixin Delegation:** Added `@Inject(method = "getOrigin", at = @At("HEAD"), cancellable = true)` in [`MixinNoiseBasedChunkGenerator.java`](common/src/main/java/etcodehome/freeterraforged/mixin/MixinNoiseBasedChunkGenerator.java) to delegate spawn search to `SpawnFinderFix` when non-`TerraForgedChunkGenerator` FTF worlds are loaded with an active `FTFRandomState`.

### 1.4 Native 26.3 Mod Compatibility Bridges (TerraBlender, Biolith, Lithostitched)
- **Dependency Upgrades:** Downloaded official 26.3 releases (`TerraBlender-fabric-26.3-26.3.0.0.9.jar`, `biolith-fabric-3.8.0-beta.2.jar`, `lithostitched-2.0.4-fabric-26.3.jar`) into `libs/` and updated `common/build.gradle` and `fabric/build.gradle` to compile against 26.3 native APIs without 1.21.1 workarounds.
- **TerraBlender Bridge:** Updated [`TerraBlenderCapabilityProvider.java`](common/src/main/java/etcodehome/freeterraforged/world/worldgen/terrablender/TerraBlenderCapabilityProvider.java) to call `region.getName()` directly, removing reflection now that 26.3 `Region.getName()` returns `Identifier` natively.
- **Biolith Bridge:**
  - In [`BiolithCriterionBridge.java`](common/src/main/java/etcodehome/freeterraforged/world/worldgen/biolith/BiolithCriterionBridge.java), removed reflection from `getCriterionTypeId(criterion)` and call `criterion.getType().getId()` directly returning native `Identifier`.
  - In [`BiolithPlacementBridge.java`](common/src/main/java/etcodehome/freeterraforged/world/worldgen/biolith/BiolithPlacementBridge.java) and [`BiolithCapabilityProvider.java`](common/src/main/java/etcodehome/freeterraforged/world/worldgen/biolith/BiolithCapabilityProvider.java), updated version contracts to natively support Biolith 26.3 releases (`3.8.0-beta.2`, `3.8.x`, `3.9.x`).
- **Lithostitched Bridge:**
  - In [`MixinLithostitchedBiomeInjectorManager.java`](common/src/main/java/etcodehome/freeterraforged/mixin/compat/MixinLithostitchedBiomeInjectorManager.java), updated `@Redirect` for `applyInjectors` to target 26.3 signature taking `RandomState` instead of 1.21.1's `DensityFunctionWrapper`, and updated redirect arguments to match 26.3's 4-argument `applyBiomeInjectors(server, registries, dimensions, seed)` method.
  - In [`LithostitchedBridgeContract.java`](common/src/main/java/etcodehome/freeterraforged/mixin/LithostitchedBridgeContract.java), updated bytecode contract assessment to validate 26.3's `(Map, Optional, Map, RandomState)V` injector call and 4-argument finalizer descriptor.

### 1.5 Mixin Subsystem (`:common` & `:fabric` — 43 Mixins, 0 errors)
- All 43 mixin configurations in [`freeterraforged-common.mixins.json`](common/src/main/resources/freeterraforged-common.mixins.json) and [`freeterraforged-fabric.mixins.json`](fabric/src/main/resources/freeterraforged-fabric.mixins.json) audited with `javap -p -s` against `minecraft-merged.jar`:
  - `MixinRandomState`: Replaced removed `sampler` field with `FTFRandomState` / `createClimateSampler(SamplerContext)` and applied `DfRewriteRule` router rewriting.
  - `MixinSurfaceSystem`: Targeted 26.3 `MaterialSystem` and modernized `ChunkPos.x()`/`z()`.
  - `MixinNoiseBasedChunkGenerator`: Injected into 26.3 `buildTerrain`, `doFill`, `buildSurface`, `generateCarvers`, and `getOrigin`.
  - `MixinWorldCarver`: Updated for flattened `WorldCarver<C>`.
  - `MixinNoiseChunk`: Updated for 26.3 constructor and sampler state fields.
  - `MixinMinecraftServer`: Injected into server startup and tick handlers.
  - `MixinChunkAccess`, `MixinChunkMap`, `MixinChunkSerializer`: Updated for 26.3 records and `SerializableChunkData`.
  - `MixinFeaturePlacer`: Handled 26.3 `Feature.place` dispatch.
  - `MixinLithostitchedBiomeInjectorManager`: Targeted 26.3 `BiomeInjectorManager` and `InjectorBiomeSource`.

### 1.6 Fabric Implementation (`:fabric` — 15 Java source files, 0 errors)
- Updated [`FTFFabric.java`](fabric/src/main/java/etcodehome/freeterraforged/fabric/FTFFabric.java) to cleanly decouple server/client runtime from datagen.
- Created [`FTFFabricDataGen.java`](fabric/src/main/java/etcodehome/freeterraforged/fabric/FTFFabricDataGen.java) for `fabric-datagen` entrypoint with explicit `(DataProvider.Factory<...>)` casts.
- Ported [`DataGenUtilImpl.java`](fabric/src/main/java/etcodehome/freeterraforged/platform/fabric/DataGenUtilImpl.java) and [`RegistryUtilImpl.java`](fabric/src/main/java/etcodehome/freeterraforged/platform/fabric/RegistryUtilImpl.java) to 26.3 dynamic registries (`RegistryDataLoader.WORLD_REGISTRIES`).
- Built production artifact: `fabric/build/libs/freeterraforged-26.3-1.0.0001-fabric-26.3.jar`.

---

## 2. Verification Results

### 2.1 Automated Unit Tests (`:common:test`)
- **Execution:** `JAVA_HOME=... ./gradlew --no-daemon :common:test :fabric:compileJava :fabric:jar`
- **Result:** `BUILD SUCCESSFUL` (all 35 tests passed, 0 failures, 0 errors, 0 skipped).
- **Archived Report:** `build/reports/preset-test-heightmaps/unit_tests_report.txt`
  - `UndergroundBiomeBandingConcurrencyTest`: 3 / 3 PASSED
  - `BiomeReplacementConfigTest`: 8 / 8 PASSED
  - `BiomeReplacementCompilerTest`: 12 / 12 PASSED
  - `VoronoiCentroidPeakTest`: 9 / 9 PASSED
  - `WorldgenMemoryLeakTest`: 3 / 3 PASSED

### 2.2 Runtime Headless Server & Preset Validation Suite
- **Environment:** Minecraft 26.3 dedicated server launched via Fabric Loader 0.19.5 with `level-type=freeterraforged:freeterraforged`, `-Dfreeterraforged.test.preset=true`, `-Dfreeterraforged.test.preset.exit=true`.
- **Result:** `ALL PRESET PERSISTENCE, RESOLUTION, HEIGHTMAP & PRESETMANAGER TESTS PASSED ON LOADER: fabric` (Exited with code 0).
- **Report & Artifacts:** `build/reports/preset-test-heightmaps/report.html`.
- **Verified Test Cases (20 / 20 PASSED):**
  1. `presetValidation.adaptiveBiomeLocate.fabric`: **PASSED** (adaptive ring & lattice locate coverage)
  2. `presetValidation.biomeLocateLivePlan.fabric`: **PASSED** (surface & cave locate comparison against vanilla)
  3. `presetValidation.terrainLocateLivePlan.fabric`: **PASSED** (raw and filtered cell locator validation)
  4. `presetValidation.checkSynchronizedRegistry.fabric`: **PASSED** (ground truth and active registry in sync)
  5. `presetValidation.spawnChunkRegularity.fabric`: **PASSED** (spawn span 78 blocks, max step 11 blocks, refined inland continent spawn location: `(1728, 2992, Y=164)`)
  6. `presetValidation.builtinRiversFallback.fabric`: **PASSED** (fallback resolution without source)
  7. `presetValidation.userSelectedDefault.fabric`: **PASSED** (configured user default overrides fallback)
  8. `presetValidation.manualSelectionOverrule.fabric`: **PASSED** (manual selection overrules user default on disk)
  9. `presetValidation.worldTypeCycling.fabric`: **PASSED** (world type cycling preserves manual preset)
  10. `presetValidation.successiveRunPreservation.fabric`: **PASSED** (server reboot preserves existing preset & PM state)
  11. `presetValidation.recreateWorldFlow.fabric`: **PASSED** (re-created worlds preserve original preset on disk & PM)
  12. `presetValidation.customFileResolution.fabric`: **PASSED** (custom file source resolves correctly)
  13. `presetValidation.multiRunSeamContinuity.fabric`: **PASSED** (max seam step 1 block, 73,984 / 73,984 identical samples)
  14. `presetValidation.inGameCommandParity.fabric`: **PASSED** (`/ftf heightmap` & `/ftf reliefmap` bit-for-bit core parity)
  15. `presetValidation.twoStageReloadSpawnPersistence.fabric`: **PASSED** (2D preview parity across 144 samples)
  16. `presetValidation.nonFtfWorldResolution.fabric`: **PASSED** (Default, Amplified, and Superflat fallback)
  17. `presetValidation.modCompatibilityShims.fabric`: **PASSED** (foreign `CellSampler` & `Climate.Sampler` shims)
  18. `presetValidation.undergroundBiomeBanding.fabric`: **PASSED** (vertical banding & 4 cave biome transitions)
  19. `presetValidation.biomeReplacementValidation.fabric`: **PASSED** (live chunk generator replacement & state restore)
  20. `presetValidation.worldgenMemoryLeak.fabric`: **PASSED** (heap retained stably across expanding live chunk batches)
- **Live Mod Compatibility Test:** Verified that `TerraBlender-fabric-26.3-26.3.0.0.9.jar` loads into the live Minecraft 26.3 server, registers overworld/nether regions cleanly (`[main/INFO]: Registered region minecraft:overworld to index 0 for type OVERWORLD`), and successfully completes all 20 / 20 validation tests with exit code 0.
