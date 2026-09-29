package io.github.milczekt1.corral.rules.spring.nosetterinjection.fixtures;

import jakarta.inject.Inject;

/** MUST FLAG: {@code jakarta.inject.Inject} on a setter. */
public class InvoiceService {

    private OrderRepository repository;

    @Inject
    public void setRepository(OrderRepository repository) {
        this.repository = repository;
    }

    public int pending() {
        return repository.count();
    }
}
