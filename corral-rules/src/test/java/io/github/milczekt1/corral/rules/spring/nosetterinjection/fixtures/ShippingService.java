package io.github.milczekt1.corral.rules.spring.nosetterinjection.fixtures;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;

/**
 * MUST FLAG the {@code jakarta.annotation.Resource} setter only: {@code @PostConstruct} shares its
 * package, so a predicate widened to the package fails.
 */
public class ShippingService {

    private OrderRepository repository;
    private int backlog;

    @Resource
    public void setRepository(OrderRepository repository) {
        this.repository = repository;
    }

    @PostConstruct
    public void warmUp() {
        backlog = repository.count();
    }

    public int backlog() {
        return backlog;
    }
}
