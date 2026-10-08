package io.github.milczekt1.corral.rules.spring.nofinalcomponent.fixtures;

import org.springframework.stereotype.Component;

/**
 * MUST IGNORE when imported from where it compiles, test output. MUST FLAG if copied into a
 * production layout, which is how the test shows the scope clause, not the annotation, spares it.
 */
@Component
public final class TestOutputFinalBean {

    public int seed() {
        return 42;
    }
}
