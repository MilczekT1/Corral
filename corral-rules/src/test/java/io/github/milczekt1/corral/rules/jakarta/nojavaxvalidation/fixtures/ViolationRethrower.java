package io.github.milczekt1.corral.rules.jakarta.nojavaxvalidation.fixtures;

import javax.validation.ConstraintViolationException;

/** MUST FLAG: the call on the caught exception, not the catch clause. */
public class ViolationRethrower {

    public void submit(Runnable submission) {
        try {
            submission.run();
        } catch (ConstraintViolationException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }
}
