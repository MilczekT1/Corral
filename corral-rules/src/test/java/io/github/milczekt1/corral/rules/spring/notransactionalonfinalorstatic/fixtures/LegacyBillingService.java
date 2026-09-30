package io.github.milczekt1.corral.rules.spring.notransactionalonfinalorstatic.fixtures;

import javax.transaction.Transactional;

/** MUST FLAG: {@code javax.transaction.Transactional} on a static method. */
public class LegacyBillingService {

    private static int charged;

    @Transactional
    public static void chargeCard() {
        charged++;
    }

    public int charged() {
        return charged;
    }
}
