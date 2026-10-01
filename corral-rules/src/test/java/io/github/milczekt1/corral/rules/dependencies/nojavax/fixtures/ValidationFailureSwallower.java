package io.github.milczekt1.corral.rules.dependencies.nojavax.fixtures;

import javax.validation.ValidationException;

/** MUST FLAG: a catch clause alone is a dependency. */
public class ValidationFailureSwallower {

    public boolean tryValidate(Runnable validation) {
        try {
            validation.run();
            return true;
        } catch (ValidationException e) {
            return false;
        }
    }
}
