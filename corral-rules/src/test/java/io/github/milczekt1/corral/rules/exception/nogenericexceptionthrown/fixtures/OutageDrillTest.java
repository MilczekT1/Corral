package io.github.milczekt1.corral.rules.exception.nogenericexceptionthrown.fixtures;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** MUST IGNORE: declares a JUnit test method, so it is a test class wherever it is compiled. */
public class OutageDrillTest {

    @Test
    void surfacesTheOutage() {
        RuntimeException outage = new RuntimeException("boom");
        assertEquals("boom", outage.getMessage());
    }
}
