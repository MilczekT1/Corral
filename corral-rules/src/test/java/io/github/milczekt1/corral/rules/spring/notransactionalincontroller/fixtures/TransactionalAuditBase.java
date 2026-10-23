package io.github.milczekt1.corral.rules.spring.notransactionalincontroller.fixtures;

import org.springframework.transaction.annotation.Transactional;

/** MUST IGNORE: not a controller; a base class annotated {@code @Transactional}, which Spring honours on subclasses. */
@Transactional
public abstract class TransactionalAuditBase {

    protected String stamp(String entry) {
        return entry + "@" + System.nanoTime();
    }
}
