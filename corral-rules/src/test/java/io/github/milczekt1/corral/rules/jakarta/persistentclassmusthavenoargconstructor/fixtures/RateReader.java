package io.github.milczekt1.corral.rules.jakarta.persistentclassmusthavenoargconstructor.fixtures;

import org.hibernate.metamodel.spi.EmbeddableInstantiator;
import org.hibernate.metamodel.spi.ValueAccess;

/** MUST IGNORE: an instantiator, not a mapped class. */
public class RateReader implements EmbeddableInstantiator {

    @Override
    public Object instantiate(ValueAccess valueAccess) {
        return new DiscountRate(valueAccess.getValue(0, Integer.class));
    }

    @Override
    public boolean isInstance(Object object) {
        return object instanceof DiscountRate;
    }

    @Override
    public boolean isSameClass(Object object) {
        return object.getClass() == DiscountRate.class;
    }
}
