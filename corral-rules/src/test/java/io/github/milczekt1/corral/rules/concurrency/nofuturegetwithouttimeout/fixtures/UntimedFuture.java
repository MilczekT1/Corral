package io.github.milczekt1.corral.rules.concurrency.nofuturegetwithouttimeout.fixtures;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

/**
 * MUST FLAG the timed {@code get}: it ignores its timeout and waits on the no-argument {@code get()},
 * so a predicate exempting every {@code get} override fails here.
 */
public class UntimedFuture extends FutureTask<String> {

    public UntimedFuture(Callable<String> task) {
        super(task);
    }

    @Override
    public String get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException {
        return get();
    }
}
