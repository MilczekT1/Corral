package io.github.milczekt1.corral.fixtures.testing;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/** Waits on the condition with a ceiling, so a fast machine finishes immediately. */
public class PollingCheckoutIT {

    private final CountDownLatch charged = new CountDownLatch(1);

    @Test
    void chargesTheCard() throws InterruptedException {
        Executors.newSingleThreadExecutor().execute(charged::countDown);

        assertTrue(charged.await(5, TimeUnit.SECONDS));
    }
}
