package io.github.milczekt1.corral.rules.spring.nosetterinjection.fixtures;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** MUST FLAG {@code setRepository} only: {@code setLabel} is a plain setter the container never calls. */
@Service
public class OrderService {

    private OrderRepository repository;
    private String label;

    @Autowired
    public void setRepository(OrderRepository repository) {
        this.repository = repository;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String describe() {
        return label + ": " + repository.count();
    }
}
