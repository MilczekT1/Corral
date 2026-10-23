package io.github.milczekt1.corral.rules.concurrency.noobjectmonitormethods.fixtures;

import java.util.function.IntSupplier;

/**
 * MUST FLAG the method reference {@code lock::notifyAll}. MUST IGNORE {@code lock::hashCode}: a
 * no-argument reference on the same receiver, so a predicate dropping the name fails here.
 */
public class WakeupRelay {

    private final Object lock = new Object();

    public int wakeAll() {
        synchronized (lock) {
            Runnable wake = lock::notifyAll;
            IntSupplier identity = lock::hashCode;
            wake.run();
            return identity.getAsInt();
        }
    }
}
