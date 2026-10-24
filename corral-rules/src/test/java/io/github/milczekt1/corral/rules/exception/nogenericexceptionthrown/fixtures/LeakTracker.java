package io.github.milczekt1.corral.rules.exception.nogenericexceptionthrown.fixtures;

/**
 * MUST FLAG {@code new Throwable} assigned to a field in a constructor of a class that is not a
 * {@code Throwable}.
 */
public class LeakTracker {

    private final Throwable allocationSite;

    public LeakTracker() {
        allocationSite = new Throwable("allocated here");
    }

    public StackTraceElement[] allocatedAt() {
        return allocationSite.getStackTrace();
    }
}
