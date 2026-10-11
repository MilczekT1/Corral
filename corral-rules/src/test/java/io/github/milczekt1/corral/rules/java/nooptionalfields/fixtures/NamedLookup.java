package io.github.milczekt1.corral.rules.java.nooptionalfields.fixtures;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.time.Clock;
import java.util.Optional;

/** MUST FLAG: Spring scans {@code @Named} as a component, but it is not meta-annotated {@code @Component}. */
@Named
public class NamedLookup {

    @Inject
    private Optional<Clock> lookupClock;

    public boolean hasLookupClock() {
        return lookupClock.isPresent();
    }
}
