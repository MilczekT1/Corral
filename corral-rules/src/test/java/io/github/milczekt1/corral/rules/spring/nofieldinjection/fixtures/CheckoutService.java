package io.github.milczekt1.corral.rules.spring.nofieldinjection.fixtures;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * MUST FLAG {@code orders} and {@code archive}, one violation each. MUST IGNORE {@code prefix}:
 * {@code @Value} shares {@code @Autowired}'s package, so a predicate widened to the package fails.
 * MUST IGNORE the plain and static fields.
 */
@Service
public class CheckoutService {

    private static final String SEPARATOR = ": ";

    @Autowired
    private OrderRepository orders;

    @Autowired
    private OrderRepository archive;

    @Value("${checkout.prefix}")
    private String prefix;

    private String label = "checkout";

    public String describe() {
        return prefix + label + SEPARATOR + (orders.count() + archive.count());
    }
}
