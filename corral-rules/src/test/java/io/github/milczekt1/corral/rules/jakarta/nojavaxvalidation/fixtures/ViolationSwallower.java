package io.github.milczekt1.corral.rules.jakarta.nojavaxvalidation.fixtures;

import javax.validation.ConstraintViolationException;

/** NOT SEEN: ArchUnit records no dependency for a catch clause, and the rule's docs say so. */
public class ViolationSwallower {

    public boolean trySubmit(Runnable submission) {
        try {
            submission.run();
            return true;
        } catch (ConstraintViolationException e) {
            return false;
        }
    }
}
