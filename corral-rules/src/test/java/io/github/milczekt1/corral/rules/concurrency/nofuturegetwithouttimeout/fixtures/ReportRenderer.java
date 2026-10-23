package io.github.milczekt1.corral.rules.concurrency.nofuturegetwithouttimeout.fixtures;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * MUST FLAG {@code render} and {@code renderNow}: each waits on {@code Future.get()} with no timeout.
 * MUST IGNORE {@code renderWithin} and {@code abandon} beside them: a timed {@code get}, and
 * {@code cancel}.
 */
public class ReportRenderer {

    private final ExecutorService executor;

    private final Future<String> pending;

    public ReportRenderer(ExecutorService executor) {
        this.executor = executor;
        this.pending = executor.submit(this::build);
    }

    public String render() throws InterruptedException, ExecutionException {
        return pending.get();
    }

    public String renderNow() throws InterruptedException, ExecutionException {
        return executor.submit(this::build).get();
    }

    public String renderWithin(long millis) throws InterruptedException, ExecutionException, TimeoutException {
        return pending.get(millis, TimeUnit.MILLISECONDS);
    }

    public boolean abandon() {
        return pending.cancel(true);
    }

    private String build() {
        return "report";
    }
}
