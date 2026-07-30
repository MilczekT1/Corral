package io.github.milczekt1.archrules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.milczekt1.archrules.groups.AllCentralRules;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** The README rules table is documentation that must not drift from the registry. */
class ReadmeRulesTableTest {

    /** Matches a leading table cell holding a backticked rule id, e.g. {@code | `db.foo` |}. */
    private static final Pattern ROW = Pattern.compile("^\\|\\s*`([a-z0-9]+(?:\\.[a-z0-9-]+)+)`\\s*\\|", Pattern.MULTILINE);

    private static String readme;

    @BeforeAll
    static void loadEverything() throws IOException {
        AllCentralRules.loadAll();
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
        Set<String> registered = new LinkedHashSet<>(RuleRegistry.all().stream().map(RuleDoc::id).toList());

        assertTrue(documentedIds().containsAll(registered),
                "README is missing rows for: " + minus(registered, documentedIds()));
    }

    @Test
    void noStaleRowsSurviveARemovedRule() {
        Set<String> registered = new LinkedHashSet<>(RuleRegistry.all().stream().map(RuleDoc::id).toList());

        assertTrue(registered.containsAll(documentedIds()),
                "README documents rules that no longer exist: " + minus(documentedIds(), registered));
    }

    @Test
    void tableAndRegistryMatchExactly() {
        Set<String> registered = new LinkedHashSet<>(RuleRegistry.all().stream().map(RuleDoc::id).toList());

        assertEquals(registered.stream().sorted().toList(), documentedIds().stream().sorted().toList());
    }

    @Test
    void readmeExplainsTheConsumerWiring() {
        assertTrue(readme.contains("ArchTests.in(AllCentralRules.class)"), "README must show the wiring");
        assertTrue(readme.contains("failureDisplayFormat"), "README must show how to enable rich failures");
        assertTrue(readme.contains("DoNotIncludeTests"),
                "README must warn against excluding test classes");
    }

    private static Set<String> minus(Set<String> a, Set<String> b) {
        Set<String> result = new LinkedHashSet<>(a);
        result.removeAll(b);
        return result;
    }
}
