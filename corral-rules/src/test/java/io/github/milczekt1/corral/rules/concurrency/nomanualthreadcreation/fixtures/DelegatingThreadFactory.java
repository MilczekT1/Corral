package io.github.milczekt1.corral.rules.concurrency.nomanualthreadcreation.fixtures;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/** MUST IGNORE: the JDK factory constructs the thread, and this one only names it. */
public class DelegatingThreadFactory implements ThreadFactory {

    private final ThreadFactory delegate = Executors.defaultThreadFactory();
    private final AtomicInteger sequence = new AtomicInteger();

    @Override
    public Thread newThread(Runnable task) {
        Thread thread = delegate.newThread(task);
        thread.setName("payments-" + sequence.incrementAndGet());
        return thread;
    }
}
