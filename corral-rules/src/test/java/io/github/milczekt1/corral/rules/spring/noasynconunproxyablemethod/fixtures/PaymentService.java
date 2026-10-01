package io.github.milczekt1.corral.rules.spring.noasynconunproxyablemethod.fixtures;

import org.springframework.scheduling.annotation.Async;

/**
 * MUST IGNORE: a class-based proxy intercepts public, protected and package-private methods alike.
 * {@code roundCents} is private but not async, and {@code describe} is final but not async.
 */
public class PaymentService {

    private long cents;

    @Async
    public void settlePayment() {
        cents = roundCents(cents);
    }

    @Async
    protected void refundPayment() {
        cents--;
    }

    @Async
    void notifyLedger() {
        cents++;
    }

    public final String describe() {
        return "payments: " + cents;
    }

    private long roundCents(long amount) {
        return amount - amount % 100;
    }
}
