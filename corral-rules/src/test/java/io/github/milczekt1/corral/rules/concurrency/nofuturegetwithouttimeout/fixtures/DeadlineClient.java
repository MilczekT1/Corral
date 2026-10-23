package io.github.milczekt1.corral.rules.concurrency.nofuturegetwithouttimeout.fixtures;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * MUST IGNORE: every wait here is bounded, already complete or not on a {@code Future}. The
 * no-argument {@code get()} of a {@code Supplier}, a {@code ThrowingSupplier} and an
 * {@code AtomicReference} fails a predicate dropping the owner check; {@code isDone()} and
 * {@code join()} fail one dropping the name check.
 */
public class DeadlineClient {

    public String bounded(Future<String> quote) throws InterruptedException, ExecutionException, TimeoutException {
        return quote.get(5, TimeUnit.SECONDS);
    }

    public String ifDone(Future<String> quote) throws InterruptedException, ExecutionException, TimeoutException {
        return quote.isDone() ? quote.get(0, TimeUnit.SECONDS) : "pending";
    }

    public CompletableFuture<String> combined(CompletableFuture<String> first, CompletableFuture<String> second) {
        return CompletableFuture.allOf(first, second).thenApply(done -> first.join() + second.join());
    }

    public String timedOut(CompletableFuture<String> quote) {
        return quote.orTimeout(5, TimeUnit.SECONDS).join();
    }

    public String supplied(Supplier<String> plain, ThrowingSupplier<String> throwing) throws Exception {
        return plain.get() + throwing.get();
    }

    public String cached(AtomicReference<String> latest) {
        return latest.get();
    }
}
