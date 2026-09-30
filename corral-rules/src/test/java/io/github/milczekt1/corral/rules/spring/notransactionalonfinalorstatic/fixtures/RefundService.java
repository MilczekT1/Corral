package io.github.milczekt1.corral.rules.spring.notransactionalonfinalorstatic.fixtures;

import org.springframework.transaction.annotation.Transactional;

/**
 * MUST FLAG {@code refund} only: the class-level annotation makes it transactional. It never makes
 * the static {@code vatRate} transactional, and {@code roundAmount} is private.
 */
@Transactional
public class RefundService {

    private int refunds;

    public final void refund() {
        refunds += roundAmount();
    }

    public int listRefunds() {
        return refunds;
    }

    public static double vatRate() {
        return 0.23;
    }

    private final int roundAmount() {
        return 1;
    }
}
