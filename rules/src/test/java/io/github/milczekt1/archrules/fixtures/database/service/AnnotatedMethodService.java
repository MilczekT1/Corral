package io.github.milczekt1.archrules.fixtures.database.service;

import org.springframework.transaction.annotation.Transactional;

public class AnnotatedMethodService {

    @Transactional
    public void doWork() {
    }

    public void untouched() {
    }
}
