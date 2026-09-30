package io.github.milczekt1.corral.rules.spring.notransactionalonprivatemethod.fixtures;

import org.springframework.transaction.annotation.Transactional;

/**
 * MUST FLAG {@code postEntries} only: every other transactional method here is public, protected
 * or package-private, and {@code audit} is private without the annotation.
 */
public class LedgerService {

    private int entries;

    @Transactional
    public void openBatch() {
        postEntries();
        audit();
    }

    @Transactional
    protected void closeBatch() {
        entries = 0;
    }

    @Transactional
    void rebalance() {
        entries++;
    }

    @Transactional
    private void postEntries() {
        entries += 2;
    }

    private void audit() {
        entries--;
    }

    public int entries() {
        return entries;
    }
}
