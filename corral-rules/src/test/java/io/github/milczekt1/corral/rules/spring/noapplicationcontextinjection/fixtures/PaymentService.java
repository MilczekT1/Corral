package io.github.milczekt1.corral.rules.spring.noapplicationcontextinjection.fixtures;

import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/** MUST IGNORE: declared dependencies, and {@code ApplicationEventPublisher} is a supertype of the container. */
@Service
public class PaymentService {

    private final Map<String, PricingStrategy> strategies;
    private final ApplicationEventPublisher events;

    public PaymentService(Map<String, PricingStrategy> strategies, ApplicationEventPublisher events) {
        this.strategies = strategies;
        this.events = events;
    }

    public long charge(String strategy, Basket basket) {
        events.publishEvent(basket);
        return strategies.get(strategy).price(basket);
    }
}
