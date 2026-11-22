package etcodehome.freeterraforged.world.worldgen.runtime;

import net.minecraft.resources.Identifier;

public interface RequestOwnedBiomeSourceFactory {
	Identifier requestOwnedFactoryId();

	RequestOwnedBiomeSource createRequestOwnedSource(PreviewSourceContext context) throws Exception;
}
