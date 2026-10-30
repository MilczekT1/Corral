package io.github.milczekt1.corral.rules.spring.configurationpropertiesmustuseprojectprefix.fixtures;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** MUST FLAG at its declaration: a custom annotation fixing a prefix outside the project prefix. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ConfigurationProperties("legacy")
public @interface LegacyBinding {
}
