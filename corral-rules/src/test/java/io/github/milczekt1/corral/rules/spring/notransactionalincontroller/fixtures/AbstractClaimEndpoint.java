package io.github.milczekt1.corral.rules.spring.notransactionalincontroller.fixtures;

import org.springframework.transaction.annotation.Transactional;

/** MUST IGNORE: not a controller; an unannotated base class whose {@code @Transactional} method two controllers inherit. */
public abstract class AbstractClaimEndpoint {

    private int settled;

    @Transactional
    public int settle() {
        return ++settled;
    }
}
