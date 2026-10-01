package io.github.milczekt1.corral.rules.spring.noasynconunproxyablemethod.fixtures;

/**
 * MUST FLAG {@code dispatchCourier} only: async through {@link AsyncWorkerBase}'s class-level
 * annotation. {@code queueDepth} is overridable.
 */
public class CourierDispatcher extends AsyncWorkerBase {

    public final void dispatchCourier() {
        queued++;
    }

    public int queueDepth() {
        return queued;
    }
}
