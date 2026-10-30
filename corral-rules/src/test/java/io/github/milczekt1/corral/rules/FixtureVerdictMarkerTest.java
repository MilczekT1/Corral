package io.github.milczekt1.corral.rules;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Every rule example under a {@code fixtures/} package opens its class Javadoc with its verdict:
 * {@code MUST FLAG}, {@code MUST IGNORE}, or {@code MIXED} when the verdict differs by member and each
 * member carries its own. A single-verdict Javadoc that also names the other verdict is a mixed
 * fixture in disguise. Collaborators belong in {@code fixtures/support/}, which is not scanned.
 *
 * <p>Fixtures that predate this check are listed in {@value #BASELINE}; the list only shrinks.
 */
class FixtureVerdictMarkerTest {

    /** Resolved against the JVM working directory, which under Surefire is the module. */
    private static final Path RULE_SOURCES = Path.of("src/test/java/io/github/milczekt1/corral/rules");

    private static final String BASELINE = "src/test/resources/fixture-verdict-marker-baseline.txt";

    private static final String FLAG = "MUST FLAG";
    private static final String IGNORE = "MUST IGNORE";
    private static final String MIXED = "MIXED";

    /** A top-level Javadoc starts in column 0; a member's is indented. */
    private static final Pattern CLASS_JAVADOC =
            Pattern.compile("^/\\*\\*(.*?)\\*/", Pattern.MULTILINE | Pattern.DOTALL);

    @Test
    void everyFixtureOpensWithItsVerdict() throws IOException {
        Set<String> unlisted = new TreeSet<>(offenders());
        unlisted.removeAll(baseline());

        assertTrue(unlisted.isEmpty(), "Open each fixture's class Javadoc with " + FLAG + ", " + IGNORE
                + " or " + MIXED + " (member markers), never naming both verdicts at class level;"
                + " move a collaborator to fixtures/support/:\n" + String.join("\n", unlisted));
    }

    @Test
    void baselineListsOnlyFixturesThatStillLackAVerdict() throws IOException {
        Set<String> settled = new TreeSet<>(baseline());
        settled.removeAll(offenders());

        assertTrue(settled.isEmpty(),
                "Now marked, moved or deleted — remove from " + BASELINE + ":\n" + String.join("\n", settled));
    }

    @Test
    void scansTheRuleFixtures() throws IOException {
        assertFalse(fixtureSources().isEmpty(), "no fixture found under " + RULE_SOURCES.toAbsolutePath());
    }

    private static Set<String> offenders() throws IOException {
        Set<String> offenders = new TreeSet<>();
        for (Path source : fixtureSources()) {
            if (!opensWithAVerdict(Files.readString(source))) {
                offenders.add(RULE_SOURCES.relativize(source).toString().replace('\\', '/'));
            }
        }
        return offenders;
    }

    private static boolean opensWithAVerdict(String source) {
        Optional<String> javadoc = classJavadoc(source);
        if (javadoc.isEmpty()) {
            return false;
        }
        String text = javadoc.get();
        return text.startsWith(MIXED)
                || text.startsWith(FLAG) && !text.contains(IGNORE)
                || text.startsWith(IGNORE) && !text.contains(FLAG);
    }

    private static Optional<String> classJavadoc(String source) {
        Matcher javadoc = CLASS_JAVADOC.matcher(source);
        if (!javadoc.find()) {
            return Optional.empty();
        }
        return Optional.of(javadoc.group(1).lines()
                .map(line -> line.strip().replaceFirst("^\\*\\s?", ""))
                .collect(Collectors.joining(" "))
                .strip());
    }

    private static List<Path> fixtureSources() throws IOException {
        try (Stream<Path> sources = Files.walk(RULE_SOURCES)) {
            return sources
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(FixtureVerdictMarkerTest::isRuleExample)
                    .toList();
        }
    }

    private static boolean isRuleExample(Path source) {
        List<String> directories = RULE_SOURCES.relativize(source.getParent()).toString().replace('\\', '/')
                .transform(path -> List.of(path.split("/")));
        int fixtures = directories.indexOf("fixtures");
        return fixtures >= 0 && !directories.subList(fixtures + 1, directories.size()).contains("support");
    }

    private static Set<String> baseline() throws IOException {
        try (Stream<String> lines = Files.lines(Path.of(BASELINE))) {
            return lines.map(String::strip)
                    .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .collect(Collectors.toCollection(TreeSet::new));
        }
    }
}
