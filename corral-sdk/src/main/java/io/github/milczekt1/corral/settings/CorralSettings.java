package io.github.milczekt1.corral.settings;

import io.github.milczekt1.corral.doc.RuleDoc;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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

    /** Rule id to the keys it read without declaring them; reported by {@code RequiredSettings}. */
    private static final Map<String, Set<String>> UNDECLARED_READS = new ConcurrentHashMap<>();

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
     * <p>A key {@code doc} does not declare with {@code requiredSetting} does not throw here — this
     * runs during class init, where the ArchUnit engine drops the cause. {@code guard()} fails the
     * rule instead, and the supplier throws {@code IllegalStateException} naming the fix. It also
     * throws when the value is absent.
     */
    public static Supplier<String> setting(RuleDoc doc, String key) {
        return setting(doc, key, CorralSettings::current);
    }

    static Supplier<String> setting(RuleDoc doc, String key, Supplier<SettingsFile> source) {
        if (!doc.requiredSettings().contains(key)) {
            UNDECLARED_READS.computeIfAbsent(doc.id(), id -> ConcurrentHashMap.newKeySet()).add(key);
            return () -> {
                throw new IllegalStateException(undeclared(doc.id(), key));
            };
        }
        return () -> source.get().value(key).orElseThrow(() -> new IllegalStateException(missing(key)));
    }

    static Set<String> undeclaredReads(String ruleId) {
        return Set.copyOf(UNDECLARED_READS.getOrDefault(ruleId, Set.of()));
    }

    static String undeclared(String ruleId, String key) {
        return "rule '" + ruleId + "' reads setting '" + key + "' without declaring it: add .requiredSetting(\""
                + key + "\") to its RuleDoc, or a missing value passes unnoticed whenever the rule matches nothing";
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
