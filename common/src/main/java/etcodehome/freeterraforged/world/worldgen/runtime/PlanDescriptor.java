package etcodehome.freeterraforged.world.worldgen.runtime;

import java.util.Objects;
import java.util.Optional;

import net.minecraft.resources.Identifier;

public record PlanDescriptor(
	Identifier id,
	WorldgenFacet facet,
	CapabilityState state,
	String mechanism,
	String detail,
	Optional<CapabilityFailure> firstCause
) {
	public PlanDescriptor {
		id = Objects.requireNonNull(id, "id");
		facet = Objects.requireNonNull(facet, "facet");
		state = Objects.requireNonNull(state, "state");
		mechanism = Objects.requireNonNull(mechanism, "mechanism");
		detail = Objects.requireNonNull(detail, "detail");
		firstCause = Objects.requireNonNull(firstCause, "firstCause");
		if (state == CapabilityState.UNAVAILABLE && firstCause.isEmpty()) {
			throw new IllegalArgumentException("Unavailable plan descriptors require a first cause");
		}
	}

	public CapabilityNodeReport report(WorldgenOwner owner) {
		return new CapabilityNodeReport(
			this.id, this.facet, this.state, this.mechanism, owner.type(), this.detail, this.firstCause
		);
	}
}
