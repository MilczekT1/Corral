package io.github.milczekt1.corral.rules.spring.noapplicationcontextinjection.fixtures;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

/** MUST FLAG the injected {@code ApplicationContext}; MUST IGNORE the {@code PricingStrategy} it looks up. */
@Service
public class OrderService {

    private final ApplicationContext context;

    public OrderService(ApplicationContext context) {
        this.context = context;
    }

    public long place(Basket basket) {
        return context.getBean(PricingStrategy.class).price(basket);
    }
}
