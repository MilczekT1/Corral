package io.github.milczekt1.corral.rules.layering.notestlibrariesinproduction.fixtures;

import java.util.concurrent.TimeUnit;
import spock.util.concurrent.BlockingVariable;

/** MUST FLAG {@code spock..}: Spock's test-only handoff variable used as a production latch. */
public class EventRelay {

    private final BlockingVariable<String> lastEvent = new BlockingVariable<>(5, TimeUnit.SECONDS);

    void publish(String event) {
        lastEvent.set(event);
    }

    String awaitNext() throws InterruptedException {
        return lastEvent.get();
    }
}
