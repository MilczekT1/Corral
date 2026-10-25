package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures;

import jakarta.validation.constraints.NotBlank;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Carries {@code @NotBlank} without being a {@code @Constraint}, so Bean Validation ignores it. */
@NotBlank
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Trimmed {
}
