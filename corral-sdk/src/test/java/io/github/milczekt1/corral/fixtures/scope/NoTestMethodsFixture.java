package io.github.milczekt1.corral.fixtures.scope;

/**
 * No JUnit test method of any kind — a helper or abstract base living in test sources. Whether it
 * counts as a test class therefore depends only on where it was compiled to, which is what makes
 * {@code TestScope}'s location half testable without mocking a {@code JavaClass}.
 */
public class NoTestMethodsFixture {

    public String helper() {
        return "not a test";
    }
}
