package io.github.milczekt1.corral.rules.concurrency.noobjectmonitormethods.fixtures;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * MUST FLAG {@code lock.wait()} and {@code lock.notifyAll()} on a plain {@code Object} field. MUST
 * IGNORE {@code pending.isEmpty()}: a no-argument call, so a predicate dropping the name fails here.
 */
public class WorkQueue {

    private final Object lock = new Object();
    private final Queue<Runnable> pending = new ArrayDeque<>();

    public Runnable awaitWork() throws InterruptedException {
        synchronized (lock) {
            while (pending.isEmpty()) {
                lock.wait();
            }
            return pending.remove();
        }
    }

    public void submit(Runnable task) {
        synchronized (lock) {
            pending.add(task);
            lock.notifyAll();
        }
    }
}
