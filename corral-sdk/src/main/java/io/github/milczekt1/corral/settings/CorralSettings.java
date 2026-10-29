package io.github.milczekt1.corral.settings;

import lombok.experimental.UtilityClass;

/**
 * The consumer's {@value #FILE}: project settings, exclusions and the ignore-patterns kill switch,
 * read once per JVM from the root of the test classpath.
 */
@UtilityClass
public class CorralSettings {

    public static final String FILE = "corral.properties";

    public static final String BASE_PACKAGE = "corral.base-package";

    public static final String IGNORE_PATTERNS_FAIL = "corral.ignore-patterns.fail";

    /** Followed by a rule id; the value is the reason the rule does not apply here. */
    public static final String EXCLUDE_PREFIX = "corral.exclude.";

    /** Never throws: a file that cannot be read is a {@link SettingsFile#isBroken() broken} one. */
    public static SettingsFile current() {
        return Current.SETTINGS;
    }

    /** {@link SettingsFile#classPathWarning()} of {@link #current()}. */
    public static String classPathWarning() {
        return current().classPathWarning();
    }

    /** Mirrors {@code ClassLoaders.getCurrentClassLoader}, how ArchUnit resolves its own files. */
    static ClassLoader currentClassLoader() {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        return contextClassLoader != null ? contextClassLoader : CorralSettings.class.getClassLoader();
    }

    private static final class Current {

        private static final SettingsFile SETTINGS = SettingsFile.load(currentClassLoader());
    }
}
