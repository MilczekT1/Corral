package io.github.milczekt1.corral.rules.concurrency.nomanualthreadcreation.fixtures;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;

/** MUST FLAG the {@code super(pool)} call: {@code ForkJoinWorkerThread} is a JDK {@code Thread} subclass. */
public class ForkJoinCounterThread extends ForkJoinWorkerThread {

    public ForkJoinCounterThread(ForkJoinPool pool) {
        super(pool);
    }
}
