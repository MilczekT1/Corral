package io.github.milczekt1.corral.rules.spring.nosetterinjection.fixtures;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** MUST IGNORE: constructor injection, including an explicit {@code @Autowired} on the constructor. */
@Service
public class PaymentService {

    private final OrderRepository repository;

    @Autowired
    public PaymentService(OrderRepository repository) {
        this.repository = repository;
    }

    public int settled() {
        return repository.count();
    }
}
