package io.github.milczekt1.corral.rules.spring.nofieldinjection.fixtures;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * MUST IGNORE when imported from where it compiles, test output: field injection is the idiom
 * there. MUST FLAG if copied into a production layout, which is how the test shows the scope
 * clause, not the annotation, is what spares it.
 */
public class TestOutputInjectedHelper {

    @Autowired
    private OrderRepository seeded;

    public int rows() {
        return seeded.count();
    }
}
