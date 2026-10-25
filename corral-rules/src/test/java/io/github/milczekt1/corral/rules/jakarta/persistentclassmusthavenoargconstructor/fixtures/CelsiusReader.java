package io.github.milczekt1.corral.rules.jakarta.persistentclassmusthavenoargconstructor.fixtures;

import org.hibernate.metamodel.spi.EmbeddableInstantiator;
import org.hibernate.metamodel.spi.ValueAccess;

/** MUST IGNORE: an instantiator, not a mapped class. */
public class CelsiusReader implements EmbeddableInstantiator {

    @Override
    public Object instantiate(ValueAccess valueAccess) {
        return new Temperature(valueAccess.getValue(0, Double.class));
    }

    @Override
    public boolean isInstance(Object object) {
        return object instanceof Temperature;
    }

    @Override
    public boolean isSameClass(Object object) {
        return object.getClass() == Temperature.class;
    }
}
