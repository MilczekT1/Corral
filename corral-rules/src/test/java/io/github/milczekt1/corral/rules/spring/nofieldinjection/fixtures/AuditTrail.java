package io.github.milczekt1.corral.rules.spring.nofieldinjection.fixtures;

import javax.inject.Inject;

/** MUST FLAG: {@code javax.inject.Inject}, for a codebase mid-migration. */
public class AuditTrail {

    @Inject
    private OrderRepository entries;

    public int recorded() {
        return entries.count();
    }
}
