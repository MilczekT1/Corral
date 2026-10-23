package io.github.milczekt1.corral.rules.concurrency.noobjectmonitormethods.fixtures;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * MUST FLAG: {@code notifyAll} on a receiver typed as an interface, which javac 18+ records as the
 * call's owner in place of {@code Object}.
 */
public class PendingJobs {

    private final Deque<String> jobs = new ArrayDeque<>();

    public void enqueue(String job) {
        synchronized (jobs) {
            jobs.offerLast(job);
            jobs.notifyAll();
        }
    }
}
