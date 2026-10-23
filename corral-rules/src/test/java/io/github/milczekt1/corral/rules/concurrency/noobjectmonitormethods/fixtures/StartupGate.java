package io.github.milczekt1.corral.rules.concurrency.noobjectmonitormethods.fixtures;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** MUST IGNORE: the replacement, a latch awaited with a timeout. */
public class StartupGate {

    private final CountDownLatch ready = new CountDownLatch(1);

    public void awaitReady() throws InterruptedException {
        if (!ready.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("not ready after 5 s");
        }
    }

    public void markReady() {
        ready.countDown();
    }
}
