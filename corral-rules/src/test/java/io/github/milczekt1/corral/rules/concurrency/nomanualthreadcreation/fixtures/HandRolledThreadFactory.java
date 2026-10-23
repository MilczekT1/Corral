package io.github.milczekt1.corral.rules.concurrency.nomanualthreadcreation.fixtures;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/** MUST FLAG {@code new Thread(r, name)}: a hand-written factory is the same unmanaged thread. */
public class HandRolledThreadFactory implements ThreadFactory {

    private final AtomicInteger sequence = new AtomicInteger();

    @Override
    public Thread newThread(Runnable task) {
        return new Thread(task, "orders-" + sequence.incrementAndGet());
    }
}
