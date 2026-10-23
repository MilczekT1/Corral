package io.github.milczekt1.corral.rules.spring.noapplicationcontextinjection.fixtures;

import org.springframework.stereotype.Service;

/** MUST IGNORE: a lookup through a static holder is not seen — a known gap. */
@Service
public class HolderClient {

    public long quote(Basket basket) {
        return SpringContextHolder.getBean(PricingStrategy.class).price(basket);
    }
}
