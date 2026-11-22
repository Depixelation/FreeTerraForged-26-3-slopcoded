package etcodehome.freeterraforged.world.worldgen.runtime;

import java.util.Objects;

import net.minecraft.resources.Identifier;

public record ProviderOrder(Identifier before, Identifier after, boolean required) {
	public ProviderOrder {
		before = Objects.requireNonNull(before, "before");
		after = Objects.requireNonNull(after, "after");
		if (before.equals(after)) {
			throw new IllegalArgumentException("A provider cannot be ordered relative to itself: " + before);
		}
	}

	public ProviderOrder(Identifier before, Identifier after) {
		this(before, after, true);
	}

	public static ProviderOrder optional(Identifier before, Identifier after) {
		return new ProviderOrder(before, after, false);
	}
}
