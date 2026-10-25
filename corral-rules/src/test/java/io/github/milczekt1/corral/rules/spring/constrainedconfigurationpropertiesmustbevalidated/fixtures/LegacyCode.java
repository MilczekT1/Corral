package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;

/** A project constraint, recognised only by its {@code javax} {@code @Constraint} meta-annotation. */
@Constraint(validatedBy = {})
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LegacyCode {
    String message() default "not a legacy code";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
