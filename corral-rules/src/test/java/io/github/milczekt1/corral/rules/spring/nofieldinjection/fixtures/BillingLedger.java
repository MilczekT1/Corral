package io.github.milczekt1.corral.rules.spring.nofieldinjection.fixtures;

import javax.annotation.Resource;

/** MUST FLAG: {@code javax.annotation.Resource}, for a codebase mid-migration. */
public class BillingLedger {

    @Resource
    private OrderRepository invoices;

    public int billed() {
        return invoices.count();
    }
}
