package io.github.milczekt1.corral.rules.spring.nofinalcomponent.fixtures;

import org.springframework.stereotype.Repository;

/** MUST FLAG: a final {@code @Repository}. */
@Repository
public final class CustomerRepository {

    public long count() {
        return 0L;
    }
}
