package io.github.milczekt1.corral.rules.java.publicstaticfieldsmustbefinal.fixtures;

/** MUST FLAG {@code lastSnapshot}: {@code volatile} alone does not exempt a field, the metamodel annotation does. */
public class MetricsRegistry {

    public static volatile String lastSnapshot = "";

    public static void snapshot(String value) {
        lastSnapshot = value;
    }
}
