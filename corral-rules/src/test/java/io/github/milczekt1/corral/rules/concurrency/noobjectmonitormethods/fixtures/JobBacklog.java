package io.github.milczekt1.corral.rules.concurrency.noobjectmonitormethods.fixtures;

import java.util.LinkedList;

/** MUST FLAG: {@code notifyAll} on a receiver typed as a concrete collection rather than {@code Object}. */
public class JobBacklog {

    private final LinkedList<String> queue = new LinkedList<>();

    public void enqueue(String job) {
        synchronized (queue) {
            queue.addLast(job);
            queue.notifyAll();
        }
    }
}
