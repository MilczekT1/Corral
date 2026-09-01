package io.github.milczekt1.corral.fixtures.testing;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/** The same wait, spelled through TimeUnit — the rewrite the anti-fix guidance warns about. */
public class TimeUnitSleepingIT {

    @Test
    void chargesTheCard() throws InterruptedException {
        TimeUnit.MILLISECONDS.sleep(500);
    }
}
