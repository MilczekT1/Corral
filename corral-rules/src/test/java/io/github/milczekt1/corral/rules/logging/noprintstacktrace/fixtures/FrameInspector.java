package io.github.milczekt1.corral.rules.logging.noprintstacktrace.fixtures;

/** MUST IGNORE: reads the frames with {@code getStackTrace()} without printing them. */
public class FrameInspector {

    public String originOf(Throwable failure) {
        StackTraceElement[] frames = failure.getStackTrace();
        return frames.length == 0 ? "unknown" : frames[0].getClassName();
    }
}
