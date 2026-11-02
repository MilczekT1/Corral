package io.github.milczekt1.corral.rules.testsupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import io.github.milczekt1.corral.rules.testsupport.FixtureVerdicts.Verdict;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FixtureVerdictsTest {

    private static final Path RULE_TESTS = FixtureVerdicts.TEST_SOURCES.resolve("io/github/milczekt1/corral/rules");

    @Test
    void everyFixtureOpensWithAVerdict() throws IOException {
        List<Path> fixtures = sourcesUnder(RULE_TESTS).stream()
                .filter(source -> source.toString().contains("/fixtures/"))
                .toList();
        assertFalse(fixtures.isEmpty(), "premise: the scan must find fixtures, or it checks nothing");

        assertEquals(List.of(), fixtures.stream().filter(source -> FixtureVerdicts.verdictOf(source).isEmpty()).toList(),
                "every fixture's type Javadoc must open with MUST FLAG, MUST IGNORE or SUPPORT");
    }

    @Test
    void everyRuleTestKeepsItsExamplesInFixtures() throws IOException {
        assertEquals(List.of(), ruleTests().stream()
                        .filter(source -> !Files.isDirectory(source.resolveSibling("fixtures")))
                        .toList(),
                "a rule test's examples go in a fixtures package beside it, never nested in the test");
    }

    @Test
    void everyRuleTestChecksItsFixtureVerdicts() throws IOException {
        assertEquals(List.of(), ruleTests().stream().filter(source -> !callsAssertMatch(source)).toList(),
                "a rule test must call FixtureVerdicts.assertMatch");
    }

    private static List<Path> ruleTests() throws IOException {
        List<Path> ruleTests = sourcesUnder(RULE_TESTS).stream()
                .filter(source -> source.getFileName().toString().endsWith("RuleTest.java"))
                .toList();
        assertFalse(ruleTests.isEmpty(), "premise: the scan must find rule tests, or it checks nothing");
        return ruleTests;
    }

    @Test
    void readsTheTypeJavadocWhenAnnotationsShareTheDeclarationLine(@TempDir Path directory) throws IOException {
        Path source = Files.writeString(directory.resolve("Cart.java"), """
                /** MUST IGNORE: the type's verdict. */
                @Entity public class Cart {
                    /** MUST FLAG: a member's verdict. */
                    private Long id;
                }
                """);

        assertEquals(Optional.of(Verdict.MUST_IGNORE), FixtureVerdicts.verdictOf(source));
    }

    @Test
    void readsNoVerdictWithoutATypeDeclarationToAnchorIt(@TempDir Path directory) throws IOException {
        Path source = Files.writeString(directory.resolve("Cart.java"), """
                /** MUST FLAG: a member's verdict, which must not be read as the type's. */
                private Long id;
                """);

        assertEquals(Optional.empty(), FixtureVerdicts.verdictOf(source));
    }

    private static boolean callsAssertMatch(Path source) {
        try {
            return Files.readString(source).contains("FixtureVerdicts.assertMatch(");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static List<Path> sourcesUnder(Path directory) throws IOException {
        try (Stream<Path> files = Files.walk(directory)) {
            return files.filter(file -> file.toString().endsWith(".java")).sorted().toList();
        }
    }
}
