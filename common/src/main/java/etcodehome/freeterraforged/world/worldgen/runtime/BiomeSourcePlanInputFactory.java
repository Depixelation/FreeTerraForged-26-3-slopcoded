package etcodehome.freeterraforged.world.worldgen.runtime;

import net.minecraft.resources.Identifier;

public interface BiomeSourcePlanInputFactory {
	Identifier biomeSourcePlanFactoryId();

	BiomeSourcePlanInput createBiomeSourcePlanInput(WorldgenOwner owner);
}
