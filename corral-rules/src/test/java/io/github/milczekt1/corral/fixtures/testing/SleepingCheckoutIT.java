package io.github.milczekt1.corral.fixtures.testing;

import org.junit.jupiter.api.Test;

/** The canonical violation: a guess about how long the submit takes, on someone else's machine. */
public class SleepingCheckoutIT {

    @Test
    void chargesTheCard() throws InterruptedException {
        Thread.sleep(500);
    }
}
