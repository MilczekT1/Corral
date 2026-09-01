package io.github.milczekt1.corral.fixtures.testing;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/** Waits on the condition with a ceiling, so a fast machine finishes immediately. */
public class PollingCheckoutIT {

    private final CountDownLatch charged = new CountDownLatch(1);

    @Test
    void chargesTheCard() throws InterruptedException {
        charged.countDown(); // stands in for the production code completing the charge

        assertTrue(charged.await(5, TimeUnit.SECONDS));
    }
}
