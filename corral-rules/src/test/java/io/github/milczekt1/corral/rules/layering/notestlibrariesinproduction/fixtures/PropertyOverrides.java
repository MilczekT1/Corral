package io.github.milczekt1.corral.rules.layering.notestlibrariesinproduction.fixtures;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ConfigurableApplicationContext;

/** MUST FLAG {@code org.springframework.boot.test}: spring-boot-starter-test left at compile scope. */
public class PropertyOverrides {

    void applySandboxDefaults(ConfigurableApplicationContext context) {
        TestPropertyValues.of("payments.mode=sandbox").applyTo(context);
    }
}
