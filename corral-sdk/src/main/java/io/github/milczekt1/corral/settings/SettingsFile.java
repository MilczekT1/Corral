package io.github.milczekt1.corral.settings;

import static io.github.milczekt1.corral.settings.CorralSettings.BASE_PACKAGE;
import static io.github.milczekt1.corral.settings.CorralSettings.EXCLUDE_PREFIX;
import static io.github.milczekt1.corral.settings.CorralSettings.FILE;
import static io.github.milczekt1.corral.settings.CorralSettings.IGNORE_PATTERNS_FAIL;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.joining;

import io.github.milczekt1.corral.doc.RuleDoc;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * One reading of {@value CorralSettings#FILE}. Exactly one of {@link #values()} and
 * {@link #problem()} carries content.
 *
 * @param values         every key with its stripped value, in file order; empty when broken or absent
 * @param source         the URL of the file in effect, or {@code null} when there is none
 * @param ignoredSources every other copy found on the classpath
 * @param problem        why the file cannot be trusted, or {@code null}
 */
public record SettingsFile(Map<String, String> values, String source, List<String> ignoredSources, String problem) {

    /** Possessive throughout, as {@code RuleDoc}'s id pattern: greedy repetition overflows the stack. */
    private static final Pattern PACKAGE_NAME =
            Pattern.compile("^[A-Za-z_$][A-Za-z0-9_$]*+(?:\\.[A-Za-z_$][A-Za-z0-9_$]*+)*+$");

    public SettingsFile {
        values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
        ignoredSources = List.copyOf(ignoredSources);
    }

    public static SettingsFile none() {
        return new SettingsFile(Map.of(), null, List.of(), null);
    }

    public static SettingsFile broken(String problem) {
        return new SettingsFile(Map.of(), null, List.of(), problem);
    }

    public boolean isBroken() {
        return problem != null;
    }

    /** Empty when the key is absent or blank. */
    public Optional<String> value(String key) {
        return Optional.ofNullable(values.get(key)).filter(value -> !value.isBlank());
    }

    /** Rule id to reason, in file order. */
    public Map<String, String> exclusionReasons() {
        Map<String, String> reasons = new LinkedHashMap<>();
        values.forEach((key, value) -> {
            if (key.startsWith(EXCLUDE_PREFIX)) {
                reasons.put(key.substring(EXCLUDE_PREFIX.length()), value);
            }
        });
        return Collections.unmodifiableMap(reasons);
    }

    /** Only the literal {@code false} disarms it — a typo leaves it armed rather than silently open. */
    public boolean ignorePatternsFail() {
        return !"false".equalsIgnoreCase(values.getOrDefault(IGNORE_PATTERNS_FAIL, "true"));
    }

    /** {@code null} unless several copies were found, or the one in effect came from inside a jar. */
    public String classPathWarning() {
        if (source == null) {
            return null;
        }
        boolean fromJar = source.startsWith("jar:");
        if (ignoredSources.isEmpty() && !fromJar) {
            return null;
        }
        return """
                %s: the copy in effect is decided by classpath order or ships inside a library, not by \
                this project. A library must not ship %s; this project's own belongs in \
                src/test/resources.
                In effect:
                  %s
                Ignored:
                %s\
                """.formatted(FILE, FILE, source, listed(ignoredSources));
    }

    /** Never throws: this runs in a static initialiser, where anything escaping becomes an Error. */
    static SettingsFile load(ClassLoader classLoader) {
        try {
            return read(classLoader);
        } catch (IOException | RuntimeException e) {
            return broken("Could not read " + FILE + " from the classpath: " + e);
        }
    }

    private static SettingsFile read(ClassLoader classLoader) throws IOException {
        List<URL> locations = Collections.list(classLoader.getResources(FILE));
        if (locations.isEmpty()) {
            return none();
        }

        URL first = locations.get(0);
        List<String> ignored = locations.subList(1, locations.size()).stream().map(URL::toString).toList();
        try (InputStream contents = first.openStream()) {
            SettingsFile parsed = parse(new String(contents.readAllBytes(), UTF_8), first.toString());
            return new SettingsFile(parsed.values, parsed.source, ignored, parsed.problem);
        }
    }

    /** Reports every problem, not only the first. */
    static SettingsFile parse(String contents, String source) {
        OrderedProperties properties = new OrderedProperties();
        try {
            properties.load(new StringReader(withoutByteOrderMark(contents)));
        } catch (IOException | IllegalArgumentException e) {
            return new SettingsFile(Map.of(), source, List.of(),
                    brokenFileMessage(source, List.of("  " + e.getMessage())));
        }

        Set<String> problems = new LinkedHashSet<>();
        properties.duplicates.forEach(key -> problems.add(
                "  " + key + " — set more than once; which value is in effect is unanswerable"));

        Map<String, String> values = new LinkedHashMap<>();
        for (String key : properties.keysInOrder) {
            String value = properties.getProperty(key).strip();
            values.put(key, value);
            problemWith(key, value).ifPresent(problem -> problems.add("  " + key + " — " + problem));
        }

        return problems.isEmpty()
                ? new SettingsFile(values, source, List.of(), null)
                : new SettingsFile(Map.of(), source, List.of(), brokenFileMessage(source, problems));
    }

    private static Optional<String> problemWith(String key, String value) {
        if (!key.startsWith("corral.") || key.equals(IGNORE_PATTERNS_FAIL)) {
            return Optional.empty();
        }
        if (key.equals(BASE_PACKAGE)) {
            return PACKAGE_NAME.matcher(value).matches()
                    ? Optional.empty()
                    : Optional.of("'" + value + "' is not a Java package name");
        }
        if (key.startsWith(EXCLUDE_PREFIX)) {
            String ruleId = key.substring(EXCLUDE_PREFIX.length());
            if (!RuleDoc.isId(ruleId)) {
                return Optional.of("'" + ruleId + "' is not the shape of a rule id — an exclusion names a"
                        + " rule by its id, which is also its freeze-store key");
            }
            return value.isBlank()
                    ? Optional.of("excluding '" + ruleId + "' needs a reason as the value — a rule dropped"
                            + " without one is indistinguishable from a rule silenced")
                    : Optional.empty();
        }
        return Optional.of("not a key Corral defines");
    }

    /** {@code String.strip()} leaves it: {@code Character.isWhitespace('\uFEFF')} is false. */
    private static String withoutByteOrderMark(String contents) {
        return contents.startsWith("\uFEFF") ? contents.substring(1) : contents;
    }

    private static String brokenFileMessage(String source, Collection<String> problems) {
        return """
                %s could not be read, so no rule is excluded or configured and every Corral rule fails \
                until it is fixed. A file that is not understood must not be trusted to remove or \
                configure a rule.
                %s
                Corral defines %s, %s and %s<rule-id>; keys outside corral. belong to your own rules.\
                """.formatted(source, String.join(System.lineSeparator(), problems),
                        BASE_PACKAGE, IGNORE_PATTERNS_FAIL, EXCLUDE_PREFIX);
    }

    private static String listed(List<String> lines) {
        return lines.isEmpty()
                ? "  (none)"
                : lines.stream().map(line -> "  " + line).collect(joining(System.lineSeparator()));
    }

    /** {@code Properties} keeps the last of two equal keys silently and forgets the file order. */
    private static final class OrderedProperties extends Properties {

        private final transient Set<String> keysInOrder = new LinkedHashSet<>();
        private final transient Set<String> duplicates = new LinkedHashSet<>();

        /** The bookkeeping sets are not content: two loads of the same entries are equal. */
        @Override
        public synchronized boolean equals(Object other) {
            return super.equals(other);
        }

        @Override
        public synchronized int hashCode() {
            return super.hashCode();
        }

        @Override
        public synchronized Object put(Object key, Object value) {
            if (!keysInOrder.add((String) key)) {
                duplicates.add((String) key);
            }
            return super.put(key, value);
        }
    }
}
