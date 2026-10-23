package io.github.milczekt1.corral.rules.concurrency.nomanualthreadcreation.fixtures;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * MUST FLAG {@code new PollingWorker()}, a project's own {@code Thread} subclass; the anonymous
 * {@code Thread}, both where it is constructed and where its constructor calls {@code Thread}'s; and
 * the {@code Thread::new} reference. MUST IGNORE {@code Executors.newFixedThreadPool}, a method call.
 */
public class WorkerPool {

    public void startPolling() {
        new PollingWorker().start();
    }

    public void startHeartbeat() {
        new Thread() {
            @Override
            public void run() {
                interrupt();
            }
        }.start();
    }

    public ExecutorService pool() {
        return Executors.newFixedThreadPool(2, Thread::new);
    }
}
