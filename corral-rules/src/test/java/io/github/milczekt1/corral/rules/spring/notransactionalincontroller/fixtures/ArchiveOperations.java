package io.github.milczekt1.corral.rules.spring.notransactionalincontroller.fixtures;

import org.springframework.transaction.annotation.Transactional;

/** MUST IGNORE: not a controller; an interface whose method is {@code @Transactional}. */
public interface ArchiveOperations {

    @Transactional
    int purge();
}
