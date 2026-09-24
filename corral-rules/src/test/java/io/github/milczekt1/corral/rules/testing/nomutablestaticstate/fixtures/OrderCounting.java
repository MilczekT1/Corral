package io.github.milczekt1.corral.rules.testing.nomutablestaticstate.fixtures;

/**
 * MUST FLAG on {@code placedOrders} only. The final instance field beside it is the must-not-match
 * half: a predicate reading one modifier instead of both finds that one too.
 */
public class OrderCounting {

    static int placedOrders;

    private final String buyerId = "acme";

    public String placeOrder() {
        placedOrders++;
        return buyerId + "-" + placedOrders;
    }
}
