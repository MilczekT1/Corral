package io.github.milczekt1.corral.rules.concurrency.nofuturegetwithouttimeout.fixtures;

import java.util.concurrent.Future;

/**
 * MUST FLAG {@code get()}: a no-argument {@code get()} that is not a {@code Future}'s, so a predicate
 * exempting any method named {@code get} fails here.
 */
public class PendingQuote implements ThrowingSupplier<String> {

    private final Future<String> quote;

    public PendingQuote(Future<String> quote) {
        this.quote = quote;
    }

    @Override
    public String get() throws Exception {
        return quote.get();
    }
}
