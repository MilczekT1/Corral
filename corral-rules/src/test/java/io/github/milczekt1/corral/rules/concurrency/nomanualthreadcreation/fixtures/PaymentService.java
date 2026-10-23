package io.github.milczekt1.corral.rules.concurrency.nomanualthreadcreation.fixtures;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/** MUST IGNORE: the pool is injected, and the {@code Thread} parameter is never constructed. */
public class PaymentService {

    private final ExecutorService executor;

    public PaymentService(ExecutorService executor) {
        this.executor = executor;
    }

    public Future<?> chargeAsync(Runnable charge) {
        return executor.submit(charge);
    }

    public boolean isOwnThread(Thread candidate) {
        return candidate.getName().startsWith("payments-");
    }
}
