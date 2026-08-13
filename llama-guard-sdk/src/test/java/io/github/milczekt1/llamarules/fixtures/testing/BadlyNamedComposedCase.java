package io.github.milczekt1.llamarules.fixtures.testing;

/**
 * Carries no JUnit annotation directly — only {@link FastTest}, which composes {@code @Test}.
 *
 * <p>Surefire will never select this class, yet the method is a real test as far as JUnit is
 * concerned. Exactly as unexecutable as {@link BadlyNamedTestCase}, so the naming rule must flag it
 * too.
 */
public class BadlyNamedComposedCase {

    @FastTest
    void surefireWillNeverRunMeEitherThroughAComposedAnnotation() {
    }
}
