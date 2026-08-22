package io.github.milczekt1.corral.fixtures.logging;

/**
 * The rule's blind spot, pinned deliberately: {@code printStackTrace()} does reach System.err, but
 * the field access happens inside java.lang.Throwable, so the calling class never touches the field
 * and neither rule reports it.
 */
public class PrintStackTraceCaller {

    public void report(Exception failure) {
        failure.printStackTrace();
    }
}
