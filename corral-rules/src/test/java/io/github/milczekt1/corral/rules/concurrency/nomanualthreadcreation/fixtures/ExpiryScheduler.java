package io.github.milczekt1.corral.rules.concurrency.nomanualthreadcreation.fixtures;

import java.util.Timer;
import java.util.TimerTask;

/**
 * MUST FLAG both {@code java.util.Timer} constructor calls, and {@code new SweepTimer()}, a project's
 * own {@code Timer} subclass. MUST IGNORE the anonymous {@code TimerTask} and the
 * {@code javax.swing.Timer}, neither of which is a {@code java.util.Timer}.
 */
public class ExpiryScheduler {

    public void expireLater(Runnable expiry) {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                expiry.run();
            }
        }, 1000);
    }

    public Timer reaper() {
        return new Timer("reaper", true);
    }

    public Timer sweeper() {
        return new SweepTimer();
    }

    public javax.swing.Timer blinker() {
        return new javax.swing.Timer(500, event -> { });
    }
}
