package io.github.milczekt1.corral.rules.spring.notransactionalonprivatemethod.fixtures;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.transaction.annotation.Transactional;

/** A project's own composed annotation; the meta-annotation is what the rule must see through. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Transactional(readOnly = true)
public @interface ReadOnlyTransaction {
}
