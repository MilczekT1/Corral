package io.github.milczekt1.corral.rules.spring.notransactionalonprivatemethod.fixtures;

import org.springframework.transaction.annotation.Transactional;

/** MUST FLAG: a private transactional method on a {@code final} class. */
public final class RefundService {

    private int refunds;

    public void refund() {
        issueRefund();
    }

    @Transactional
    private void issueRefund() {
        refunds++;
    }

    public int refunds() {
        return refunds;
    }
}
