package etcodehome.freeterraforged.fabric;

import etcodehome.freeterraforged.client.command.FTFCommands;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import etcodehome.freeterraforged.FTFCommon;
import etcodehome.freeterraforged.fabric.network.FTFFabricNetworking;
import etcodehome.freeterraforged.server.FTFMinecraftServer;
import etcodehome.freeterraforged.world.worldgen.runtime.WorldgenLifecycle;
import etcodehome.freeterraforged.world.worldgen.runtime.WorldgenResourceRevision;

public class FTFFabric implements ModInitializer {

	@Override
	public void onInitialize() {
		FTFCommon.bootstrap();
		FTFFabricNetworking.init();
		ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resources, success) -> {
			if (!success) {
				return;
			}
			((FTFMinecraftServer) server).getFeatureTemplateManager().onReload(resources);
			WorldgenLifecycle.tagsReloaded(
				server, ((WorldgenResourceRevision) server).advanceWorldgenResourceRevision()
			);
		});

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			FTFCommands.register(dispatcher);
		});
	}
}
