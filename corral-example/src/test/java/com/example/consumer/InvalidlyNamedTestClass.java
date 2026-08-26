package com.example.consumer;

import org.junit.jupiter.api.Test;

/** Violates test.class-names-must-end-with-test-or-it: holds a @Test method but ends in neither Test, Tests nor IT. */
class InvalidlyNamedTestClass {

    @Test
    void checksSomethingNobodyRuns() {
    }
}
