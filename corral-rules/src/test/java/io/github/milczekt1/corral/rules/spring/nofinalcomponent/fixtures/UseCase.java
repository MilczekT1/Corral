package io.github.milczekt1.corral.rules.spring.nofinalcomponent.fixtures;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.stereotype.Service;

/**
 * A project's own stereotype; the meta-annotation chain is what the rule must see through. MUST
 * IGNORE the annotation type itself, which is not final.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Service
public @interface UseCase {
}
