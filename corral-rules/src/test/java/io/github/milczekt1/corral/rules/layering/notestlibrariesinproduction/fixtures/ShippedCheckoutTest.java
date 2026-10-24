package io.github.milczekt1.corral.rules.layering.notestlibrariesinproduction.fixtures;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** MUST FLAG from production output: declaring a JUnit test method does not exempt a class there. */
public class ShippedCheckoutTest {

    @Test
    void totalsTheBasket() {
        assertEquals(3, Math.addExact(1, 2));
    }
}
