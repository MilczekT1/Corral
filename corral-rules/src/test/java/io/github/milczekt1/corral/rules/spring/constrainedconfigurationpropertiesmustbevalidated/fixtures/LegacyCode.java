package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;

/** Not an example: a project constraint meta-annotated with {@code javax} {@code @Constraint}. */
@Constraint(validatedBy = {})
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LegacyCode {
    String message() default "not a legacy code";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
