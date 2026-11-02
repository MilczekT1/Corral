package io.github.milczekt1.corral.rules.java.nopublicstaticarrayfields.fixtures.production;

/** MUST IGNORE: enum constants are not arrays, and the compiler's {@code $VALUES} array is private. */
public enum Priority {
    HIGH,
    LOW;

    public boolean isUrgent() {
        return this == HIGH;
    }
}
