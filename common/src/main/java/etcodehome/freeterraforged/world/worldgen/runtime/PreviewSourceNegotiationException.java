package etcodehome.freeterraforged.world.worldgen.runtime;

import java.util.Objects;
import java.util.Optional;

import net.minecraft.resources.Identifier;

public final class PreviewSourceNegotiationException extends IllegalStateException {
	private final Optional<Identifier> provider;
	private final CapabilityFailure failure;

	public PreviewSourceNegotiationException(
		Optional<Identifier> provider,
		CapabilityFailure failure
	) {
		this(provider, failure, null);
	}

	public PreviewSourceNegotiationException(
		Optional<Identifier> provider,
		CapabilityFailure failure,
		Throwable cause
	) {
		super(failure.code() + ": " + failure.message(), cause);
		this.provider = Objects.requireNonNull(provider, "provider");
		this.failure = Objects.requireNonNull(failure, "failure");
	}

	public Optional<Identifier> provider() {
		return this.provider;
	}

	public CapabilityFailure failure() {
		return this.failure;
	}
}
