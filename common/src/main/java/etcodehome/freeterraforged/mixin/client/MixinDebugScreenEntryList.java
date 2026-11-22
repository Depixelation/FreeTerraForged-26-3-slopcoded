package etcodehome.freeterraforged.mixin.client;

import java.util.List;
import java.util.Map;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.client.gui.components.debug.DebugScreenEntryList;
import net.minecraft.client.gui.components.debug.DebugScreenEntryStatus;
import net.minecraft.resources.Identifier;

import etcodehome.freeterraforged.client.debug.DebugEntryFreeTerraForged;

@Mixin(DebugScreenEntryList.class)
public abstract class MixinDebugScreenEntryList {

    @Shadow
    @Final
    private Map<Identifier, DebugScreenEntryStatus> allStatuses;

    @Shadow
    @Final
    private List<Identifier> currentlyEnabled;

    @Shadow
    private boolean isOverlayVisible;

    @Shadow
    public abstract DebugScreenEntryStatus getStatus(Identifier id);

    @Inject(method = "getStatus", at = @At("HEAD"), cancellable = true)
    private void freeterraforged$defaultEntryStatus(Identifier id, CallbackInfoReturnable<DebugScreenEntryStatus> cir) {
        if (DebugEntryFreeTerraForged.ID.equals(id) && !this.allStatuses.containsKey(id)) {
            cir.setReturnValue(DebugScreenEntryStatus.IN_OVERLAY);
        }
    }

    @Inject(
        method = "rebuildCurrentList",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/List;sort(Ljava/util/Comparator;)V"
        )
    )
    private void freeterraforged$includeTerrainDebug(CallbackInfo ci) {
        if (!this.currentlyEnabled.contains(DebugEntryFreeTerraForged.ID)) {
            DebugScreenEntryStatus status = this.getStatus(DebugEntryFreeTerraForged.ID);
            if (status == DebugScreenEntryStatus.ALWAYS_ON || (this.isOverlayVisible && status == DebugScreenEntryStatus.IN_OVERLAY)) {
                DebugScreenEntry entry = DebugScreenEntries.getEntry(DebugEntryFreeTerraForged.ID);
                if (entry != null && entry.isAllowed(Minecraft.getInstance().showOnlyReducedInfo())) {
                    this.currentlyEnabled.add(DebugEntryFreeTerraForged.ID);
                }
            }
        }
    }
}
