package io.github.milczekt1.corral.rules.java.nointernalapi.fixtures;

import sun.reflect.ReflectionFactory;

/** MUST FLAG: a call into {@code sun.reflect}, the other package {@code jdk.unsupported} exports. */
public class SerializationInstantiator {

    public Object factory() {
        return ReflectionFactory.getReflectionFactory();
    }
}
