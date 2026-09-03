package io.github.milczekt1.corral.rules.testing.testclassnamingconvention.fixtures;

/**
 * MUST FLAG: carries no JUnit annotation directly — only {@link FastTest}, which composes
 * {@code @Test}. JUnit still treats the method as a real test.
 */
public class BadlyNamedComposedCase {

    @FastTest
    void surefireWillNeverRunMeThroughAComposedAnnotation() {
    }
}
