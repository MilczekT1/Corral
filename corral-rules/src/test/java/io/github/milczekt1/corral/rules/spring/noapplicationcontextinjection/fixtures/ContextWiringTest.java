package io.github.milczekt1.corral.rules.spring.noapplicationcontextinjection.fixtures;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

/** MUST IGNORE: declares a JUnit test method, so it is a test class wherever it is compiled. */
public class ContextWiringTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void wiresAPricingStrategy() {
        assertNotNull(context.getBean(PricingStrategy.class));
    }
}
