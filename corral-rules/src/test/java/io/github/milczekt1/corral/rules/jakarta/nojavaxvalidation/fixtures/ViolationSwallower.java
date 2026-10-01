package io.github.milczekt1.corral.rules.jakarta.nojavaxvalidation.fixtures;

import javax.validation.ConstraintViolationException;

/** MUST FLAG: a catch clause alone is a dependency. */
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
