package io.github.milczekt1.corral.rules.spring.nocomponentonabstractclass.fixtures.production;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.stereotype.Service;

/**
 * MUST IGNORE the annotation type itself, which is abstract in bytecode: a project's own stereotype, and
 * the meta-annotation chain is what the rule must see through.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Service
public @interface DomainService {
}
