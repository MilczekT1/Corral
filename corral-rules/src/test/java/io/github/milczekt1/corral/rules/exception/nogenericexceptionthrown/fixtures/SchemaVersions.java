package io.github.milczekt1.corral.rules.exception.nogenericexceptionthrown.fixtures;

/** MUST FLAG {@code new Error}. MUST IGNORE {@code new AssertionError}, the type for a state that cannot happen. */
public class SchemaVersions {

    public int migrate(int version) {
        if (version > 3) {
            throw new Error("unknown schema version " + version);
        }
        return version + 1;
    }

    public int columnsIn(int version) {
        return switch (version) {
            case 1, 2 -> 4;
            case 3 -> 5;
            default -> throw new AssertionError("unreachable");
        };
    }
}
