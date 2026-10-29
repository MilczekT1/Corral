package io.github.milczekt1.corral.settings;

import io.github.milczekt1.corral.doc.RuleDoc;
import java.util.function.Supplier;
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

    /**
     * The value of {@code key}, looked up on every {@code get()} — so read it while the rule is
     * evaluated, never while its class initialises. Never put the value in a description or a
     * violation message: those are freeze-store matching keys, and changing the setting would
     * resurface every frozen violation.
     *
     * @throws IllegalArgumentException when {@code doc} does not declare {@code key} with
     *                                  {@code requiredSetting}; the supplier throws
     *                                  {@code IllegalStateException} when the value is absent
     */
    public static Supplier<String> setting(RuleDoc doc, String key) {
        return setting(doc, key, CorralSettings::current);
    }

    static Supplier<String> setting(RuleDoc doc, String key, Supplier<SettingsFile> source) {
        if (!doc.requiredSettings().contains(key)) {
            throw new IllegalArgumentException("rule '" + doc.id() + "' reads setting '" + key
                    + "' without declaring it: add .requiredSetting(\"" + key + "\") to its RuleDoc, or a"
                    + " missing value passes unnoticed whenever the rule matches nothing");
        }
        return () -> source.get().value(key).orElseThrow(() -> new IllegalStateException(missing(key)));
    }

    static String missing(String key) {
        return key + " is not set. Add it to src/test/resources/" + FILE + ".";
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
