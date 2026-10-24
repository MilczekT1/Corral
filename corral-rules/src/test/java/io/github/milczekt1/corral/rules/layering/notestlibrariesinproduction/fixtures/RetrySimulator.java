package io.github.milczekt1.corral.rules.layering.notestlibrariesinproduction.fixtures;

import java.time.Duration;
import reactor.test.scheduler.VirtualTimeScheduler;

/** MUST FLAG {@code reactor.test..}: reactor-test's virtual clock driving a production simulator. */
public class RetrySimulator {

    void fastForward(Duration backoff) {
        VirtualTimeScheduler.getOrSet().advanceTimeBy(backoff);
    }
}
