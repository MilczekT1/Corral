package io.github.milczekt1.corral.rules.spring.nofinalcomponent.fixtures;

import org.springframework.stereotype.Service;

/** MUST FLAG: a final {@code @Service}. */
@Service
public final class OrderService {

    public int pending() {
        return 0;
    }
}
