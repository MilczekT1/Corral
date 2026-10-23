package io.github.milczekt1.corral.rules.spring.noapplicationcontextinjection.fixtures;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.stereotype.Service;

/** MUST FLAG: a {@code BeanFactory} subtype that is not an {@code ApplicationContext}. */
@Service
public class CatalogService {

    private final ListableBeanFactory beans;

    public CatalogService(ListableBeanFactory beans) {
        this.beans = beans;
    }

    public int strategyCount() {
        return beans.getBeanNamesForType(PricingStrategy.class).length;
    }
}
