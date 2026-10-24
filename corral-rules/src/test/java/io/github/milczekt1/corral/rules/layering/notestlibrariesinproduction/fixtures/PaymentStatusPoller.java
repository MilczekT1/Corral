package io.github.milczekt1.corral.rules.layering.notestlibrariesinproduction.fixtures;

import java.time.Duration;
import java.util.function.BooleanSupplier;
import org.awaitility.Awaitility;

/** MUST FLAG {@code org.awaitility..}: Awaitility as a production polling loop. */
public class PaymentStatusPoller {

    void awaitSettled(BooleanSupplier settled) {
        Awaitility.await().atMost(Duration.ofSeconds(30)).until(settled::getAsBoolean);
    }
}
