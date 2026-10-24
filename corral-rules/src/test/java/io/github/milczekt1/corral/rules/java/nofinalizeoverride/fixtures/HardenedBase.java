package io.github.milczekt1.corral.rules.java.nofinalizeoverride.fixtures;

/** MUST FLAG {@code finalize()}: the empty final hardening idiom, since the body is not read. */
@SuppressWarnings("deprecation")
public class HardenedBase {

    @Override
    protected final void finalize() {
        // Empty on purpose: stops a subclass installing a finalizer.
    }
}
