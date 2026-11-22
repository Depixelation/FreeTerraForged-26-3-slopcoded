package etcodehome.freeterraforged.mixin.client;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.client.gui.components.debug.DebugScreenEntryStatus;
import net.minecraft.client.gui.components.debug.DebugScreenProfile;
import net.minecraft.resources.Identifier;

import etcodehome.freeterraforged.client.debug.DebugEntryFreeTerraForged;

@Mixin(DebugScreenEntries.class)
public abstract class MixinDebugScreenEntries {

    @Shadow
    private static Identifier register(Identifier id, DebugScreenEntry entry) {
        throw new AssertionError();
    }

    @Shadow
    @Final
    @Mutable
    public static Map<DebugScreenProfile, Map<Identifier, DebugScreenEntryStatus>> PROFILES;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void freeterraforged$registerDebugEntry(CallbackInfo ci) {
        register(DebugEntryFreeTerraForged.ID, new DebugEntryFreeTerraForged());

        Map<Identifier, DebugScreenEntryStatus> defaultProfile = new HashMap<>();
        Map<Identifier, DebugScreenEntryStatus> existingDefault = PROFILES.get(DebugScreenProfile.DEFAULT);
        if (existingDefault != null) {
            defaultProfile.putAll(existingDefault);
        }
        defaultProfile.put(DebugEntryFreeTerraForged.ID, DebugScreenEntryStatus.IN_OVERLAY);

        Map<DebugScreenProfile, Map<Identifier, DebugScreenEntryStatus>> newProfiles = new EnumMap<>(DebugScreenProfile.class);
        newProfiles.putAll(PROFILES);
        newProfiles.put(DebugScreenProfile.DEFAULT, Map.copyOf(defaultProfile));
        PROFILES = Map.copyOf(newProfiles);
    }
}
