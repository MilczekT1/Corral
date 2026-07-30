package io.github.milczekt1.archrules.fixtures.database.service;

import org.springframework.transaction.annotation.Transactional;

/** A second class-level violation, used to simulate a newly introduced violation. */
@Transactional
public class SecondAnnotatedService {
    public void doWork() {
    }
}
