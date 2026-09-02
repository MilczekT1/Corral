package io.github.milczekt1.corral.fixtures.testing;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/** Calls Thread and TimeUnit without waiting: the owners match, the method name does not. */
public class ClockReadingIT {

    @Test
    void readsTheDeadlineAndTheRunningThread() {
        assertTrue(TimeUnit.SECONDS.toMillis(5) > 0, Thread.currentThread().getName());
    }
}
