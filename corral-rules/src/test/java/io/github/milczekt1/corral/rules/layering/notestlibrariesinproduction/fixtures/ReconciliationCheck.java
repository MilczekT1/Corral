package io.github.milczekt1.corral.rules.layering.notestlibrariesinproduction.fixtures;

import java.util.Objects;
import org.opentest4j.TestAbortedException;

/** MUST FLAG {@code org.opentest4j..}: the exception types JUnit 5 and AssertJ throw. */
public class ReconciliationCheck {

    void run(String ledger) {
        if (Objects.isNull(ledger)) {
            throw new TestAbortedException("no ledger to reconcile");
        }
    }
}
