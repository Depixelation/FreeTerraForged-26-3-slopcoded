package etcodehome.freeterraforged.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import etcodehome.freeterraforged.client.debug.FlowFieldDebugRenderer;
import etcodehome.freeterraforged.fabric.network.FTFFabricClientNetworking;

public class FTFFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        FTFFabricClientNetworking.init();

        LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN.register(context -> {
            FlowFieldDebugRenderer.render(
                    context.poseStack(),
                    context.gameRenderer().mainCamera(),
                    context.submitNodeCollector()
            );
        });
    }
}
