package io.github.milczekt1.corral.rules.java.publicstaticfieldsmustbefinal.fixtures;

/** MUST FLAG {@code hits}: the field's own modifiers are read, not the package-private class's. */
class InternalCounters {

    public static int hits;

    static void hit() {
        hits++;
    }
}
