package io.github.milczekt1.corral.rules.concurrency.noobjectmonitormethods.fixtures;

import java.util.concurrent.TimeUnit;

/**
 * MUST FLAG {@code TimeUnit.timedWait}, which calls {@code Object.wait} on the lock. MUST IGNORE
 * {@code TimeUnit.toMillis}: same owner, so a predicate dropping the name fails here.
 */
public class ShutdownSignal {

    private final Object lock = new Object();
    private volatile boolean stopped;

    public long awaitStop() throws InterruptedException {
        long waitedMillis = 0;
        synchronized (lock) {
            while (!stopped) {
                TimeUnit.SECONDS.timedWait(lock, 1);
                waitedMillis += TimeUnit.SECONDS.toMillis(1);
            }
        }
        return waitedMillis;
    }

    public void stop() {
        stopped = true;
    }
}
