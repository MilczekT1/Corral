package io.github.milczekt1.corral.rules.spring.novalueonfield.fixtures;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

/** MUST IGNORE: declares a JUnit test method, so it is a test class wherever it is compiled. */
class LocalPortTest {

    @Value("${local.server.port}")
    private int port;

    @Test
    void listens() {
        assertTrue(port > 0);
    }
}
