package io.github.milczekt1.corral.rules.java.nooptionalfields.fixtures;

import java.time.Clock;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** MUST IGNORE: a field-injected optional dependency of a {@code @Service}; the annotation plays no part. */
@Service
public class CheckoutService {

    @Autowired
    private Optional<Clock> checkoutClock;

    public boolean timesCheckout() {
        return checkoutClock.isPresent();
    }
}
