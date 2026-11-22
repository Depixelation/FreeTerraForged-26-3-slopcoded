package etcodehome.freeterraforged.mixin;

import etcodehome.freeterraforged.world.worldgen.ChunkFlowField;
import etcodehome.freeterraforged.world.worldgen.IFlowFieldHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.PalettedContainerFactory;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import net.minecraft.world.level.chunk.storage.SerializableChunkData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SerializableChunkData.class)
public abstract class MixinChunkSerializer {
	@Shadow
	@Final
	private CompoundTag structureData;

	@Inject(method = "copyOf", at = @At("RETURN"))
	private static void freeterraforged$captureFlowField(ServerLevel level, ChunkAccess chunk, CallbackInfoReturnable<SerializableChunkData> cir) {
		SerializableChunkData data = cir.getReturnValue();
		if (chunk instanceof IFlowFieldHolder holder) {
			ChunkFlowField flowField = holder.freeterraforged$getFlowField();
			if (flowField != null && data != null && data.structureData() != null) {
				flowField.writeToNbt(data.structureData());
			}
		}
	}

	@Inject(method = "write", at = @At("RETURN"))
	private void freeterraforged$writeFlowField(CallbackInfoReturnable<CompoundTag> cir) {
		CompoundTag rootTag = cir.getReturnValue();
		if (rootTag != null && this.structureData != null && ChunkFlowField.hasData(this.structureData)) {
			rootTag.put("RTFFlowField", this.structureData.get("RTFFlowField"));
		}
	}

	@Inject(method = "parse", at = @At("RETURN"))
	private static void freeterraforged$parseFlowField(LevelHeightAccessor heightAccessor, PalettedContainerFactory factory, CompoundTag tag, CallbackInfoReturnable<SerializableChunkData> cir) {
		SerializableChunkData data = cir.getReturnValue();
		if (data != null && ChunkFlowField.hasData(tag) && data.structureData() != null) {
			data.structureData().put("RTFFlowField", tag.get("RTFFlowField"));
		}
	}

	@Inject(method = "read", at = @At("RETURN"))
	private void freeterraforged$applyFlowField(ServerLevel level, PoiManager poiManager, RegionStorageInfo regionStorageInfo, ChunkPos pos, CallbackInfoReturnable<ProtoChunk> cir) {
		ProtoChunk returnedChunk = cir.getReturnValue();
		if (returnedChunk != null && this.structureData != null && ChunkFlowField.hasData(this.structureData)) {
			ChunkAccess targetChunk = returnedChunk;
			if (returnedChunk instanceof ImposterProtoChunk imposter) {
				targetChunk = imposter.getWrapped();
			}
			if (targetChunk instanceof IFlowFieldHolder holder) {
				holder.freeterraforged$getOrCreateFlowField().readFromNbt(this.structureData);
			}
		}
	}
}
