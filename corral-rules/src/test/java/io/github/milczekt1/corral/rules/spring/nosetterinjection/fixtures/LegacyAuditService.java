package io.github.milczekt1.corral.rules.spring.nosetterinjection.fixtures;

import javax.inject.Inject;

/** MUST FLAG: {@code javax.inject.Inject}, for a codebase mid-migration. */
public class LegacyAuditService {

    private OrderRepository repository;

    @Inject
    public void setRepository(OrderRepository repository) {
        this.repository = repository;
    }

    public int audited() {
        return repository.count();
    }
}
