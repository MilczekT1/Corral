package io.github.milczekt1.corral.rules.testing.nothreadsleep.fixtures;

/**
 * MUST FLAG {@code awaitTheCharge}. MUST IGNORE {@code waitingThread}: same owner, not a sleep, so an
 * over-broad predicate fails.
 */
public class ThreadSleeper {

    public void awaitTheCharge() throws InterruptedException {
        Thread.sleep(500);
    }

    public String waitingThread() {
        return Thread.currentThread().getName();
    }
}
