package io.github.milczekt1.corral.rules.concurrency.nofuturegetwithouttimeout.fixtures;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * MUST FLAG every method: {@code blocking} calls {@code CompletableFuture.get()}, {@code deferred}
 * hands a {@code quote::get} reference on, {@code wrapped} calls {@code get()} inside a lambda, and
 * {@code traced} calls the {@code get()} of a project's own {@code Future}.
 */
public class QuoteAggregator {

    public String blocking(CompletableFuture<String> quote) throws InterruptedException, ExecutionException {
        return quote.get();
    }

    public ThrowingSupplier<String> deferred(Future<String> quote) {
        return quote::get;
    }

    public ThrowingSupplier<String> wrapped(Future<String> quote) {
        return () -> quote.get().trim();
    }

    public String traced(TracedFuture<String> quote) throws InterruptedException, ExecutionException {
        return quote.get();
    }
}
