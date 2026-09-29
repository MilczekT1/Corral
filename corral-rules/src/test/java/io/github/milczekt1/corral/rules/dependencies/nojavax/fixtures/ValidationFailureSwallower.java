package io.github.milczekt1.corral.rules.dependencies.nojavax.fixtures;

import javax.validation.ValidationException;

/** NOT SEEN: ArchUnit records no dependency for a catch clause, and the rule's docs say so. */
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
