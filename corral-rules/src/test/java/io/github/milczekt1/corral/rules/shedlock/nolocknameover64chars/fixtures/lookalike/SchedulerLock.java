package io.github.milczekt1.corral.rules.shedlock.nolocknameover64chars.fixtures.lookalike;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** SUPPORT: a project's own annotation sharing ShedLock's simple name. */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SchedulerLock {

    String name();
}
