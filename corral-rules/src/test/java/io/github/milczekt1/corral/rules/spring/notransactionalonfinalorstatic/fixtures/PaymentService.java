package io.github.milczekt1.corral.rules.spring.notransactionalonfinalorstatic.fixtures;

import org.springframework.transaction.annotation.Transactional;

/**
 * MUST FLAG {@code settle} and {@code exchangeRate} only: {@code authorize} is overridable,
 * {@code summarize} is final but not transactional, {@code recalculateFees} is private.
 */
public class PaymentService {

    private int settled;

    @Transactional
    public final void settle() {
        settled += recalculateFees();
    }

    @Transactional
    public void authorize() {
        settled--;
    }

    public final String summarize() {
        return "payments: " + settled;
    }

    @Transactional
    public static double exchangeRate() {
        return 1.0;
    }

    @Transactional
    private final int recalculateFees() {
        return 2;
    }
}
