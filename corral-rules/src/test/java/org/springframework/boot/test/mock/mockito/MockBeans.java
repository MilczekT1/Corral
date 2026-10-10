package org.springframework.boot.test.mock.mockito;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Stand-in for Spring Boot 3.x's type of this name, which Boot 4.0 removed. Only its FQN matters. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface MockBeans {

    MockBean[] value();
}
