package io.github.milczekt1.corral.rules.spring.notransactionalonfinalorstatic.fixtures;

import org.springframework.transaction.annotation.Transactional;

/**
 * MUST FLAG {@code postJournal} because the class is final, and {@code closePeriod} as final, not as
 * final-class. {@code balance} is not transactional and {@code verifyTotals} is private.
 */
public final class LedgerService {

    private int entries;

    @Transactional
    public void postJournal() {
        entries++;
        verifyTotals();
    }

    public int balance() {
        return entries;
    }

    @Transactional
    private void verifyTotals() {
        entries += 0;
    }

    @Transactional
    public final void closePeriod() {
        entries = 0;
    }
}
