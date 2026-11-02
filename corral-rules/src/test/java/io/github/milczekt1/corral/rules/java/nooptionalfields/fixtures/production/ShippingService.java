package io.github.milczekt1.corral.rules.java.nooptionalfields.fixtures.production;

import java.time.Clock;
import java.util.Optional;
import org.springframework.stereotype.Service;

/** MUST FLAG {@code Quote.carrier}, whose class is not a stereotyped bean; MUST IGNORE {@code tracker}, a field of one. */
@Service
public class ShippingService {

    private final Optional<Clock> tracker;

    public ShippingService(Optional<Clock> tracker) {
        this.tracker = tracker;
    }

    public Quote quote() {
        return new Quote(tracker.map(Clock::toString));
    }

    public static class Quote {

        private final Optional<String> carrier;

        Quote(Optional<String> carrier) {
            this.carrier = carrier;
        }

        public String carrierOrNone() {
            return carrier.orElse("none");
        }
    }
}
