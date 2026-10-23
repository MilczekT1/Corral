package io.github.milczekt1.corral.rules.spring.noapplicationcontextinjection.fixtures;

/** MUST FLAG: calls the inherited container directly. */
public class ReportJob extends LookupSupport {

    public long run(Basket basket) {
        return getApplicationContext().getBean(PricingStrategy.class).price(basket);
    }
}
