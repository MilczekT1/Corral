package io.github.milczekt1.archrules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.milczekt1.archrules.testsupport.PublishedRules;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * The README rules table is documentation that must not drift from the rules consumers run.
 *
 * <p>Compared against {@link PublishedRules}, <em>not</em> {@code RuleRegistry.all()}: the registry
 * is process-wide static state in a JVM that Surefire reuses, so sibling tests which register
 * throwaway docs (see {@code RuleRegistryTest}, {@code FrozenRulesTest}) would otherwise make this
 * class pass or fail depending on run order.
 */
class ReadmeRulesTableTest {

    /** Matches a leading table cell holding a backticked rule id, e.g. {@code | `db.foo` |}. */
    private static final Pattern ROW = Pattern.compile("^\\|\\s*`([a-z0-9]+(?:\\.[a-z0-9-]+)+)`\\s*\\|", Pattern.MULTILINE);

    private static String readme;

    @BeforeAll
    static void loadReadme() throws IOException {
        readme = Files.readString(readmePath());
    }

    /** Surefire runs with basedir = the module directory, so the repo root is one level up. */
    private static Path readmePath() {
        Path fromModule = Path.of("..", "README.md");
        return Files.exists(fromModule) ? fromModule : Path.of("README.md");
    }

    private static Set<String> documentedIds() {
        Set<String> ids = new LinkedHashSet<>();
        Matcher matcher = ROW.matcher(readme);
        while (matcher.find()) {
            ids.add(matcher.group(1));
        }
        return ids;
    }

    @Test
    void everyRuleIsDocumented() {
        Set<String> published = PublishedRules.idSet();

        assertTrue(documentedIds().containsAll(published),
                "README is missing rows for: " + minus(published, documentedIds()));
    }

    @Test
    void noStaleRowsSurviveARemovedRule() {
        Set<String> published = PublishedRules.idSet();

        assertTrue(published.containsAll(documentedIds()),
                "README documents rules that no longer exist: " + minus(documentedIds(), published));
    }

    @Test
    void tableAndPublishedRulesMatchExactly() {
        Set<String> published = PublishedRules.idSet();

        assertEquals(published.stream().sorted().toList(), documentedIds().stream().sorted().toList());
    }

    @Test
    void readmeExplainsTheConsumerWiring() {
        assertTrue(readme.contains("ArchTests.in(AllCentralRules.class)"), "README must show the wiring");
        assertTrue(readme.contains("failureDisplayFormat"), "README must show how to enable rich failures");
        assertTrue(readme.contains("DoNotIncludeTests"),
                "README must warn against excluding test classes");
    }

    @Test
    void readmeShowsHowToResolveTheArtifact() {
        // The Install block is useless if it cannot resolve: GitHub Packages needs an explicit
        // <repositories> entry plus authenticated credentials, even for public reads.
        assertTrue(readme.contains("https://maven.pkg.github.com/MilczekT1/LLamaRules"),
                "README must show the GitHub Packages repository the artifact is published to");
        assertTrue(readme.contains("settings.xml"),
                "README must point at the settings.xml server/token requirement");
    }

    @Test
    void readmeDocumentsSeedingAsADeliberateOneOffRatherThanAPermanentFlag() {
        assertTrue(readme.contains("-Darchunit.freeze.store.default.allowStoreCreation=true"),
                "README must show seeding as a one-time command-line override");
        assertTrue(readme.lines().noneMatch(line -> line.strip().equals("freeze.store.default.allowStoreCreation=true")),
                "README must not tell consumers to commit allowStoreCreation=true — that turns a missing "
                        + "store into a silent re-seed instead of a loud failure");
    }

    @Test
    void readmeGrowthPathCoversTheStepsThatSilentlyBreakEnforcementIfSkipped() {
        assertTrue(readme.contains("AllCentralRules.groups()"), "growth path must mention groups()");
        assertTrue(readme.contains("@ArchTest ArchTests"),
                "growth path must mention the @ArchTest ArchTests field — a group registered only in "
                        + "groups() is never evaluated by any consumer");
        assertTrue(readme.contains("publishesExactlyTheSeededFirstCutRules"),
                "growth path must mention the test that pins the published id set");
    }

    private static Set<String> minus(Set<String> a, Set<String> b) {
        Set<String> result = new LinkedHashSet<>(a);
        result.removeAll(b);
        return result;
    }
}
