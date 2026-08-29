package io.github.milczekt1.corral.fixtures.scope;

/**
 * No JUnit test method of any kind — a helper or abstract base living in test sources, so only
 * where it was compiled to can decide whether it is a test class.
 */
public class NoTestMethodsFixture {

    public String helper() {
        return "not a test";
    }
}
