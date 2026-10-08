package io.github.milczekt1.corral.rules.spring.nofinalcomponent.fixtures;

import org.springframework.stereotype.Indexed;

/**
 * MUST IGNORE: {@code @Indexed} is itself a meta-annotation of {@code @Component} and shares its
 * package, but does not make a bean, so a predicate widened to the package fails.
 */
@Indexed
public final class IndexedLookup {

    public String key() {
        return "lookup";
    }
}
