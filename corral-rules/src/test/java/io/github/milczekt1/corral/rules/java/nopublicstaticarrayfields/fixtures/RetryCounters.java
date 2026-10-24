package io.github.milczekt1.corral.rules.java.nopublicstaticarrayfields.fixtures;

/** MUST FLAG the non-final array {@code attempts}; MUST IGNORE the non-final, non-array {@code enabled}. */
public class RetryCounters {

    public static int[] attempts = new int[3];

    public static boolean enabled = true;

    public static void record(int slot) {
        if (enabled) {
            attempts[slot]++;
        }
    }
}
