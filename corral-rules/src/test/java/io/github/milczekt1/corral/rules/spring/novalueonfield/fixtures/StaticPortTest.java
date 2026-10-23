package io.github.milczekt1.corral.rules.spring.novalueonfield.fixtures;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

/** MUST IGNORE: a test class with a static field, so a production clause bound to {@code areNotFinal()} alone would flag it. */
class StaticPortTest {

    @Value("${local.server.port}")
    private static int port;

    @Test
    void listens() {
        assertTrue(port > 0);
    }
}
