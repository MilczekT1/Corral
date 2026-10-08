package io.github.milczekt1.corral.rules.spring.nocomponentonabstractclass.fixtures;

import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.stereotype.Component;

/** MUST IGNORE: declares a {@code @Lookup} method, so Spring subclasses it into a bean. */
@Component
public abstract class AbstractPrototypeFactory {

    @Lookup
    protected abstract StringBuilder newBuilder();

    public String build() {
        return newBuilder().append("built").toString();
    }
}
