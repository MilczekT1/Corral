package io.github.milczekt1.corral.rules.spring.novalueonfield.fixtures;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import org.springframework.beans.factory.annotation.Value;

/** MUST IGNORE: declares no field; the project's own annotation carrying {@code @Value} that {@link ShippingService} uses. */
@Retention(RUNTIME)
@Target(FIELD)
@Value("${app.timeout}")
public @interface AppTimeout {
}
