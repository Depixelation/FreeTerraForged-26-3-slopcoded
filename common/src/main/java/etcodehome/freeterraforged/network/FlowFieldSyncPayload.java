package etcodehome.freeterraforged.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ChunkPos;

public record FlowFieldSyncPayload(ChunkPos pos, byte[] rawGrid) implements CustomPacketPayload {

    public static final Type<FlowFieldSyncPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath("freeterraforged", "flow_sync")
    );

    public static final StreamCodec<FriendlyByteBuf, FlowFieldSyncPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeLong(payload.pos.pack());
                buf.writeByteArray(payload.rawGrid);
			},
			buf -> new FlowFieldSyncPayload(ChunkPos.unpack(buf.readLong()), buf.readByteArray(256))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
