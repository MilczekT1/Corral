package io.github.milczekt1.corral.fixtures.scope;

/**
 * A class with no JUnit test method of any kind — the shape of a fixture, a helper or an abstract
 * base that lives in test sources but declares no test of its own.
 *
 * <p>Carries the whole weight of the location half of {@code TestScope}: whether this class counts
 * as a test class depends only on where it was compiled to, so importing it from one build layout
 * or another is what makes the URI matching testable without mocking a {@code JavaClass}.
 */
public class NoTestMethodsFixture {

    public String helper() {
        return "not a test";
    }
}
