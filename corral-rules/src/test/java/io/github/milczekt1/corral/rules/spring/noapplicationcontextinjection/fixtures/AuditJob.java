package io.github.milczekt1.corral.rules.spring.noapplicationcontextinjection.fixtures;

/** MUST IGNORE: a lookup through the base class's own helper is not seen — a known gap. */
public class AuditJob extends LookupSupport {

    public long run(Basket basket) {
        return bean(PricingStrategy.class).price(basket);
    }
}
