package io.github.milczekt1.corral.rules.testing.nothreadsleep.fixtures;

/** A sleep parked on a helper that declares no test of its own: still test code, still matched. */
public class SleepingTestSupport {

    protected void pause(long millis) throws InterruptedException {
        Thread.sleep(millis);
    }
}
