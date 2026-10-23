package io.github.milczekt1.corral.rules.spring.norepositoryincontroller.fixtures;

import org.springframework.web.bind.annotation.RestController;

/**
 * MUST IGNORE when imported from where it compiles, test output. MUST FLAG if copied into a
 * production layout, which is how the test shows the scope clause, not the dependency, spares it.
 */
@RestController
public class TestOutputController {

    private final OrderRepository orders;

    public TestOutputController(OrderRepository orders) {
        this.orders = orders;
    }

    public long count() {
        return orders.count();
    }
}
