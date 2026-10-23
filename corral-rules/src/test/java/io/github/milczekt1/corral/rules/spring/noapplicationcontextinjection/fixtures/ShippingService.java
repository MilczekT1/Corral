package io.github.milczekt1.corral.rules.spring.noapplicationcontextinjection.fixtures;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/** MUST IGNORE: {@code ObjectProvider} is the declared way to resolve a dependency lazily. */
@Service
public class ShippingService {

    private final ObjectProvider<PricingStrategy> strategy;

    public ShippingService(ObjectProvider<PricingStrategy> strategy) {
        this.strategy = strategy;
    }

    public long cost(Basket basket) {
        return strategy.getObject().price(basket);
    }
}
