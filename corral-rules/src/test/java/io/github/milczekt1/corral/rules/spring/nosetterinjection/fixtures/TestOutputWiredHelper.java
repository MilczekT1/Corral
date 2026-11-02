package io.github.milczekt1.corral.rules.spring.nosetterinjection.fixtures;

import io.github.milczekt1.corral.rules.spring.nosetterinjection.fixtures.production.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * MUST IGNORE when imported from where it compiles, test output: the container owns test
 * instances. MUST FLAG if copied into a production layout, which is how the test shows the scope
 * clause, not the annotation, is what spares it.
 */
public class TestOutputWiredHelper {

    private OrderRepository repository;

    @Autowired
    public void setRepository(OrderRepository repository) {
        this.repository = repository;
    }

    public int seeded() {
        return repository.count();
    }
}
