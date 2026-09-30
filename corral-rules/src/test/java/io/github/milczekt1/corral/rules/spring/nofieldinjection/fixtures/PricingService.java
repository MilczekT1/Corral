package io.github.milczekt1.corral.rules.spring.nofieldinjection.fixtures;

import java.time.Clock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** MUST IGNORE: constructor injection, with {@code @Autowired} picking one of two constructors. */
@Service
public class PricingService {

    private final OrderRepository rates;
    private final Clock clock;

    @Autowired
    public PricingService(OrderRepository rates) {
        this(rates, Clock.systemUTC());
    }

    PricingService(OrderRepository rates, Clock clock) {
        this.rates = rates;
        this.clock = clock;
    }

    public long quotedAt() {
        return rates.count() + clock.millis();
    }
}
