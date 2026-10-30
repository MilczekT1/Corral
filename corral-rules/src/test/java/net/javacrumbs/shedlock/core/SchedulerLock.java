package net.javacrumbs.shedlock.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * WORKAROUND: a hand-written stand-in for ShedLock 4.x's type of this name, which ShedLock 5.0 removed,
 * so {@code corral-rules} needs no {@code shedlock-core} 4.x jar on its test classpath. Only the
 * fully-qualified name is evidence; copy it from the real jar, since a typo shared with the rule passes.
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface SchedulerLock {

    String name() default "";

    long lockAtMostFor() default -1;

    String lockAtMostForString() default "";

    long lockAtLeastFor() default -1;

    String lockAtLeastForString() default "";
}
