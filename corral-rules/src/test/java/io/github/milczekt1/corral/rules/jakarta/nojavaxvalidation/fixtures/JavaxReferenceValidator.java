package io.github.milczekt1.corral.rules.jakarta.nojavaxvalidation.fixtures;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.constraints.NotBlank;

/** MUST FLAG: a validator on the old interface is never resolved against the new one. */
public class JavaxReferenceValidator implements ConstraintValidator<NotBlank, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value != null && !value.isBlank();
    }
}
