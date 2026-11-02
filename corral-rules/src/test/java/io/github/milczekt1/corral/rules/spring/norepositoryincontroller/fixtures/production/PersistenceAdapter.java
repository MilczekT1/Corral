package io.github.milczekt1.corral.rules.spring.norepositoryincontroller.fixtures.production;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.stereotype.Repository;

/** SUPPORT: a project's own stereotype composed from {@code @Repository}. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repository
public @interface PersistenceAdapter {
}
