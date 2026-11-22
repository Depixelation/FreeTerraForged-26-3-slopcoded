package etcodehome.freeterraforged.mixin;

import etcodehome.freeterraforged.network.FlowFieldSyncPayload;
import etcodehome.freeterraforged.network.FlowSettingsSyncPayload;
import etcodehome.freeterraforged.network.FlowSettingsSyncState;
import etcodehome.freeterraforged.world.worldgen.ChunkFlowField;
import etcodehome.freeterraforged.world.worldgen.FlowSettingsSnapshot;
import etcodehome.freeterraforged.world.worldgen.IFlowFieldHolder;
import etcodehome.freeterraforged.world.worldgen.IFlowSettingsHolder;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.PlayerChunkSender;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerChunkSender.class)
public class MixinPlayerChunkSender {
	@Unique
	private final FlowSettingsSyncState freeterraforged$flowSettings = new FlowSettingsSyncState();

	@Inject(method = "sendNextChunks", at = @At("HEAD"))
	private void freeterraforged$syncFlowSettings(ServerPlayer player, CallbackInfo callback) {
		ServerLevel level = player.level();
		FlowSettingsSnapshot settings = ((IFlowSettingsHolder) level).freeterraforged$getFlowSettings();
		if (this.freeterraforged$flowSettings.update(level.dimension(), settings)) {
			player.connection.send(new ClientboundCustomPayloadPacket(new FlowSettingsSyncPayload(settings)));
		}
	}

	@Inject(
		method = "sendChunk",
		at = @At("TAIL")
	)
	private static void onSendChunk(ServerGamePacketListenerImpl listener, ServerLevel level, LevelChunk chunk, CallbackInfo ci) {
		if (chunk instanceof IFlowFieldHolder holder) {
			ChunkFlowField flowField = holder.freeterraforged$getFlowField();

			if (flowField != null && flowField.hasRivers()) {
				listener.send(new ClientboundCustomPayloadPacket(
					new FlowFieldSyncPayload(chunk.getPos(), flowField.getRawGrid())
				));
			}
		}
	}
}
