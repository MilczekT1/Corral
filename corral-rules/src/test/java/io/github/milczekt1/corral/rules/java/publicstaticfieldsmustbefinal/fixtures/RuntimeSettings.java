package io.github.milczekt1.corral.rules.java.publicstaticfieldsmustbefinal.fixtures;

/** MUST FLAG {@code endpoint}; MUST IGNORE the constants, the private, package-private and protected statics, and the instance field. */
public class RuntimeSettings {

    public static final String NAME = "runtime";

    public static final String[] FALLBACK_HOSTS = {"backup-1", "backup-2"};

    public static String endpoint = "https://localhost";

    private static String cache;

    static String packageLevel;

    protected static String inherited;

    public String label = "default";

    public static String describe() {
        cache = endpoint + packageLevel + inherited + FALLBACK_HOSTS.length;
        return NAME + cache;
    }

    /** MUST FLAG {@code ceiling}: a nested class's line carries the {@code $}-separated binary name. */
    public static class Limits {

        public static long ceiling = 100L;

        public static boolean exceeds(long value) {
            return value > ceiling;
        }
    }
}
