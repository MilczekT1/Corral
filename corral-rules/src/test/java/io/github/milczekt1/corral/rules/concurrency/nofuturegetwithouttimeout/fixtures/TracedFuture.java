package io.github.milczekt1.corral.rules.concurrency.nofuturegetwithouttimeout.fixtures;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * MUST FLAG {@code get()}: a delegating {@code Future} cannot pick a deadline, so its delegation is
 * frozen as debt. MUST IGNORE the timed {@code get}, {@code cancel}, {@code isCancelled} and
 * {@code isDone} delegations, so a predicate dropping the name or parameter check fails here.
 */
public class TracedFuture<T> implements Future<T> {

    private final Future<T> delegate;

    public TracedFuture(Future<T> delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return delegate.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        return delegate.isCancelled();
    }

    @Override
    public boolean isDone() {
        return delegate.isDone();
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        return delegate.get();
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return delegate.get(timeout, unit);
    }
}
