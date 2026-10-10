package org.springframework.boot.test.mock.mockito;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * WORKAROUND: a hand-written stand-in for Spring Boot 3.x's type of this name, which Boot 4.0 removed,
 * so {@code corral-rules} needs no {@code spring-boot-test} 3.x jar on its test classpath. Only the
 * fully-qualified name is evidence; copy it from the real jar, since a typo shared with the rule passes.
 * Keep {@code reset()}: the committed store records the {@link MockReset} member it adds.
 */
@Target({ElementType.TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(SpyBeans.class)
public @interface SpyBean {

    Class<?>[] value() default {};

    MockReset reset() default MockReset.AFTER;
}
