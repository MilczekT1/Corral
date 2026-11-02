package io.github.milczekt1.corral.rules.spring.nocomponentonabstractclass.fixtures.production;

import org.springframework.beans.factory.annotation.Lookup;

/** MUST IGNORE: abstract with a {@code @Lookup} method, but no stereotype. */
public abstract class AbstractLookupSupport {

    @Lookup
    protected abstract StringBuilder freshBuffer();
}
