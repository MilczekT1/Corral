package io.github.milczekt1.corral.rules.spring.nosetterinjection.fixtures;

import javax.annotation.Resource;

/** MUST FLAG: {@code javax.annotation.Resource}, for a codebase mid-migration. */
public class LegacyBillingService {

    private OrderRepository repository;

    @Resource
    public void setRepository(OrderRepository repository) {
        this.repository = repository;
    }

    public int billed() {
        return repository.count();
    }
}
