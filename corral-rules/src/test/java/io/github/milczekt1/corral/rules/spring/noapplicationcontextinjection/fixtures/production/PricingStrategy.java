package io.github.milczekt1.corral.rules.spring.noapplicationcontextinjection.fixtures.production;

/** MUST IGNORE: the collaborator the examples look up or inject. */
public interface PricingStrategy {

    long price(Basket basket);
}
