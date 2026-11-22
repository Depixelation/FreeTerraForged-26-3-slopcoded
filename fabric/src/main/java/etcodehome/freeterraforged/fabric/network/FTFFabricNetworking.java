package etcodehome.freeterraforged.fabric.network;

import java.lang.reflect.Method;
import etcodehome.freeterraforged.network.FlowFieldSyncPayload;
import etcodehome.freeterraforged.network.FlowSettingsSyncPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;

public class FTFFabricNetworking {

    @SuppressWarnings("unchecked")
    private static PayloadTypeRegistry<RegistryFriendlyByteBuf> getPlayS2CRegistry() {
        try {
            Method m = PayloadTypeRegistry.class.getMethod("clientboundPlay");
            return (PayloadTypeRegistry<RegistryFriendlyByteBuf>) m.invoke(null);
        } catch (NoSuchMethodException e) {
            try {
                Method m = PayloadTypeRegistry.class.getMethod("playS2C");
                return (PayloadTypeRegistry<RegistryFriendlyByteBuf>) m.invoke(null);
            } catch (ReflectiveOperationException ex) {
                throw new RuntimeException("Could not obtain playS2C payload registry", ex);
            }
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Could not obtain playS2C payload registry", e);
        }
    }

    public static void init() {
        PayloadTypeRegistry<RegistryFriendlyByteBuf> registry = getPlayS2CRegistry();
        registry.register(FlowFieldSyncPayload.TYPE, FlowFieldSyncPayload.CODEC);
        registry.register(FlowSettingsSyncPayload.TYPE, FlowSettingsSyncPayload.CODEC);
    }
}
