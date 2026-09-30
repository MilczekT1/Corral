package io.github.milczekt1.corral.rules.spring.notransactionalonprivatemethod.fixtures;

import javax.transaction.Transactional;

/** MUST FLAG: {@code javax.transaction.Transactional}, for a codebase on Spring 5 or mid-migration. */
public class LegacyBillingService {

    private int charged;

    public void bill() {
        chargeCard();
    }

    @Transactional
    private void chargeCard() {
        charged++;
    }

    public int charged() {
        return charged;
    }
}
