package io.github.milczekt1.corral.rules.concurrency.nomanualthreadcreation.fixtures;

import java.util.Timer;

/** MUST FLAG the {@code super("sweep", true)} call to {@code Timer}. */
public class SweepTimer extends Timer {

    public SweepTimer() {
        super("sweep", true);
    }
}
