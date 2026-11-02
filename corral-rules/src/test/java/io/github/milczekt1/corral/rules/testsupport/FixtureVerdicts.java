package io.github.milczekt1.corral.rules.testsupport;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.ArchRule;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.experimental.UtilityClass;

/**
 * Checks a rule's report against the verdict each of its fixtures opens with. A fixture's type
 * Javadoc starts with {@code MUST FLAG}, {@code MUST IGNORE} or {@code SUPPORT}.
 *
 * <p>A violation is attributed to the fixture its text names first: ArchUnit and Corral's own messages
 * both name the reported element before any target. A nested or anonymous class, and a member, count
 * as their enclosing fixture.
 */
@UtilityClass
public class FixtureVerdicts {

    public enum Verdict {
        /** Reported at least once. */
        MUST_FLAG,
        /** Never the reported element; it may still appear as a violation's target. */
        MUST_IGNORE,
        /** Not an example: a type the examples need to compile or resolve. Never checked. */
        SUPPORT
    }

    /** Resolved against the JVM working directory, which under Surefire is the module. */
    public static final Path TEST_SOURCES = Path.of("src/test/java");

    private static final Pattern TYPE_DECLARATION = Pattern.compile(
            "^(?:@[\\w.]+(?:\\([^)\\n]*\\))?\\s+)*"
                    + "(?:public |protected |abstract |final |sealed |non-sealed |static |strictfp )*"
                    + "(?:class|interface|enum|record|@interface) ",
            Pattern.MULTILINE);
    private static final Pattern JAVADOC = Pattern.compile("/\\*\\*(.*?)\\*/", Pattern.DOTALL);
    private static final Pattern PACKAGE = Pattern.compile("^package ([\\w.]+);", Pattern.MULTILINE);

    /**
     * Asserts that every {@code MUST FLAG} and {@code MUST IGNORE} fixture under {@code ruleTest}'s
     * {@code fixtures} package is among {@code examples} and is reported, or not, as it says.
     *
     * @param definition the raw rule: the published field is frozen, so it would report nothing
     */
    public static void assertMatch(Class<?> ruleTest, ArchRule definition, JavaClasses examples) {
        Map<String, Verdict> verdicts = verdictsUnder(ruleTest);
        assertFalse(verdicts.isEmpty(), () -> "no fixtures found for " + ruleTest.getName());

        List<String> violations = definition.allowEmptyShould(true).evaluate(examples).getFailureReport().getDetails();
        Map<String, Pattern> mentions = verdicts.keySet().stream()
                .collect(Collectors.toMap(fixture -> fixture, FixtureVerdicts::mentionOf));
        Map<String, List<String>> reported = violations.stream()
                .flatMap(violation -> reportedFixture(violation, mentions)
                        .map(fixture -> Map.entry(fixture, violation)).stream())
                .collect(Collectors.groupingBy(Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())));

        List<String> mismatches = new ArrayList<>();
        verdicts.forEach((fixture, verdict) -> {
            List<String> lines = reported.getOrDefault(fixture, List.of());
            if (verdict == Verdict.SUPPORT) {
                return;
            }
            if (!examples.contain(fixture)) {
                mismatches.add(fixture + " is " + verdict.name().replace('_', ' ') + " but never imported, so nothing checks it");
            } else if (verdict == Verdict.MUST_FLAG && lines.isEmpty()) {
                mismatches.add(fixture + " is MUST FLAG but is not reported");
            } else if (verdict == Verdict.MUST_IGNORE && !lines.isEmpty()) {
                mismatches.add(fixture + " is MUST IGNORE but is reported:\n    " + String.join("\n    ", lines));
            }
        });
        assertTrue(mismatches.isEmpty(),
                () -> String.join("\n", mismatches) + "\n\nFull report:\n" + String.join("\n", violations));
    }

    /**
     * The verdict {@code source}'s type Javadoc opens with; empty when there is none, or no type
     * declaration to anchor it, since the file's last Javadoc may then be a member's.
     */
    public static Optional<Verdict> verdictOf(Path source) {
        String text = read(source);
        Matcher type = TYPE_DECLARATION.matcher(text);
        if (!type.find()) {
            return Optional.empty();
        }
        Matcher javadoc = JAVADOC.matcher(text.substring(0, type.start()));
        String last = null;
        while (javadoc.find()) {
            last = javadoc.group(1);
        }
        if (last == null) {
            return Optional.empty();
        }
        String opening = last.lines()
                .map(line -> line.strip().replaceFirst("^\\*\\s?", ""))
                .collect(Collectors.joining(" "))
                .strip();
        return Stream.of(Verdict.values())
                .filter(verdict -> opening.startsWith(verdict.name().replace('_', ' ')))
                .findFirst();
    }

    /** Every fixture source under {@code ruleTest}'s {@code fixtures} package, sub-packages included. */
    public static List<Path> fixtureSources(Class<?> ruleTest) {
        Path fixtures = TEST_SOURCES.resolve(ruleTest.getPackageName().replace('.', '/')).resolve("fixtures");
        if (!Files.isDirectory(fixtures)) {
            return List.of();
        }
        try (Stream<Path> files = Files.walk(fixtures)) {
            return files.filter(file -> file.toString().endsWith(".java")).sorted().toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Map<String, Verdict> verdictsUnder(Class<?> ruleTest) {
        Map<String, Verdict> verdicts = new TreeMap<>();
        for (Path source : fixtureSources(ruleTest)) {
            Verdict verdict = verdictOf(source).orElseThrow(() -> new AssertionError(
                    source + " opens with no MUST FLAG, MUST IGNORE or SUPPORT verdict"));
            verdicts.put(qualifiedNameOf(source), verdict);
        }
        return verdicts;
    }

    private static Optional<String> reportedFixture(String violation, Map<String, Pattern> mentions) {
        return mentions.entrySet().stream()
                .flatMap(fixture -> firstMention(violation, fixture.getValue()).stream()
                        .mapToObj(at -> Map.entry(at, fixture.getKey())))
                .min(Comparator.comparingInt(Map.Entry::getKey))
                .map(Map.Entry::getValue);
    }

    /** Matches {@code fixture} as a whole name, or as the owner of a member or nested class. */
    private static Pattern mentionOf(String fixture) {
        return Pattern.compile("(?<![\\w.$])" + Pattern.quote(fixture) + "(?!\\w)");
    }

    private static OptionalInt firstMention(String violation, Pattern mention) {
        Matcher matcher = mention.matcher(violation);
        return matcher.find() ? OptionalInt.of(matcher.start()) : OptionalInt.empty();
    }

    private static String qualifiedNameOf(Path source) {
        Matcher packageName = PACKAGE.matcher(read(source));
        String simpleName = source.getFileName().toString().replaceFirst("\\.java$", "");
        return packageName.find() ? packageName.group(1) + "." + simpleName : simpleName;
    }

    private static String read(Path source) {
        try {
            return Files.readString(source);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
