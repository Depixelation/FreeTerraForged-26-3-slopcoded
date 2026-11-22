package etcodehome.freeterraforged.mixin;

import java.util.function.BooleanSupplier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.LevelLoadListener;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.ServerLevelData;
import etcodehome.freeterraforged.data.worldgen.preset.PresetManager;
import etcodehome.freeterraforged.data.worldgen.preset.settings.Preset;
import etcodehome.freeterraforged.data.worldgen.preset.settings.SpawnType;
import etcodehome.freeterraforged.data.worldgen.preset.settings.WorldSettings;
import etcodehome.freeterraforged.registries.FTFRegistries;
import etcodehome.freeterraforged.test.PresetValidationRunner;
import etcodehome.freeterraforged.world.worldgen.perf.WorldgenBenchmarkRunner;
import etcodehome.freeterraforged.world.worldgen.runtime.WorldgenResourceRevision;

@Mixin(MinecraftServer.class)
class MixinMinecraftServer implements WorldgenResourceRevision {
	@Unique
	private long freeterraforged$worldgenResourceRevision;

	@Override
	public long worldgenResourceRevision() {
		return this.freeterraforged$worldgenResourceRevision;
	}

	@Override
	public long advanceWorldgenResourceRevision() {
		return ++this.freeterraforged$worldgenResourceRevision;
	}

	@Inject(
		at = @At("TAIL"),
		method = "setInitialSpawn"
	)
	private static void freeterraforged$applyUserSelectedSpawn(
		ServerLevel serverLevel,
		ServerLevelData serverLevelData,
		boolean bl,
		boolean bl2,
		LevelLoadListener levelLoadListener,
		CallbackInfo callback
	) {
		if (bl) {
			return;
		}
		WorldSettings.Properties properties = serverLevel.registryAccess().lookup(FTFRegistries.PRESET)
			.flatMap(registry -> registry.get(Preset.KEY))
			.map(preset -> preset.value().world().properties)
			.orElseGet(() -> PresetManager.PM.isLoaded ? PresetManager.PM.worldSettings.properties : null);

		if (properties != null && properties.spawnType == SpawnType.USER_SELECTED) {
			int x = properties.spawnX;
			int z = properties.spawnZ;
			int y = serverLevel.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
			if (y < serverLevel.getMinY() + 1) {
				y = serverLevel.getSeaLevel() + 1;
			}
			while (y < serverLevel.getMaxY() && (serverLevel.getBlockState(new BlockPos(x, y, z)).isSolid() || serverLevel.getBlockState(new BlockPos(x, y + 1, z)).isSolid())) {
				y++;
			}
			serverLevelData.setSpawn(LevelData.RespawnData.of(serverLevel.dimension(), new BlockPos(x, y, z), 0.0f, 0.0f));
		}
	}

	@Inject(at = @At("TAIL"), method = "tickServer")
	private void freeterraforged$onTickServer(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
		WorldgenBenchmarkRunner.onServerTick((MinecraftServer) (Object) this);
		PresetValidationRunner.onServerTick((MinecraftServer) (Object) this);
	}
}
