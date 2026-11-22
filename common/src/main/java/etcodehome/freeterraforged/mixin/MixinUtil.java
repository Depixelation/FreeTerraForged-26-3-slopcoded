package etcodehome.freeterraforged.mixin;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import etcodehome.freeterraforged.concurrent.ThreadPools;
import etcodehome.freeterraforged.concurrent.cache.Cache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.util.Util;

@Mixin(Util.class)
public class MixinUtil {

	@Inject(method = "shutdownExecutors()V", at = @At("TAIL"))
	private static void shutdownExecutors(CallbackInfo callback) {
		freeterraforged$shutdownExecutor(ThreadPools.WORLD_GEN);
		freeterraforged$shutdownExecutor(Cache.SCHEDULER);
	}

	@Unique
	private static void freeterraforged$shutdownExecutor(ExecutorService executorService) {
		boolean interrupted = false;
		executorService.shutdown();
		try {
			if (!executorService.awaitTermination(3L, TimeUnit.SECONDS)) {
				executorService.shutdownNow();
			}
		} catch (InterruptedException e) {
			executorService.shutdownNow();
			interrupted = true;
		}
		if (interrupted) {
			Thread.currentThread().interrupt();
		}
	}
}
