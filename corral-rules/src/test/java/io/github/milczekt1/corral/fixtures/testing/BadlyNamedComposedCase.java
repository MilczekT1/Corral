package io.github.milczekt1.corral.fixtures.testing;

/**
 * Carries no JUnit annotation directly — only {@link FastTest}, which composes {@code @Test}.
 *
 * <p>Surefire will never select this class, yet JUnit treats the method as a real test — as
 * unexecutable as {@link BadlyNamedTestCase}.
 */
public class BadlyNamedComposedCase {

    @FastTest
    void surefireWillNeverRunMeEitherThroughAComposedAnnotation() {
    }
}
