package io.github.milczekt1.corral.rules.logging.nosystemerr.fixtures;

/**
 * MUST IGNORE, a known blind spot: {@code printStackTrace()} reaches System.err from inside
 * {@code java.lang.Throwable}, so the calling class never touches the field.
 */
public class PrintStackTraceCaller {

    public void report(Exception failure) {
        failure.printStackTrace();
    }
}
