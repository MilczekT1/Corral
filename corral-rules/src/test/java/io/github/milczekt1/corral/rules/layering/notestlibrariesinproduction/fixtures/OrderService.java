package io.github.milczekt1.corral.rules.layering.notestlibrariesinproduction.fixtures;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Objects;

/** MUST FLAG {@code org.junit..}: a JUnit Jupiter assertion used as an argument check. */
public class OrderService {

    String place(String order) {
        assertNotNull(order);
        return Objects.toString(order);
    }
}
