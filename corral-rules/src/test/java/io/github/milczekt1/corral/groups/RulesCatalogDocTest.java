package io.github.milczekt1.corral.groups;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.junit.ArchTests;
import io.github.milczekt1.corral.reflect.PublishedRules;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * Keeps {@code docs/rules.md} in step with what {@link EveryPublishedGroup} publishes: every id in
 * the table exists, every published id is in the table, and each row names the group the rule
 * actually ships in.
 *
 * <p>The table is the consumer-facing catalog and the only place a rule is described in prose, so
 * drift there is the most visible form of catalog rot — and the one a reviewer is least likely to
 * spot in a diff that touches Java.
 */
class RulesCatalogDocTest {

    /** Resolved from the module directory, where Surefire runs; the reactor root is the fallback. */
    private static final List<Path> CANDIDATE_LOCATIONS = List.of(
            Path.of("..", "docs", "rules.md"),
            Path.of("docs", "rules.md"));

    /** A catalog row: the id in the first cell and the group in the second, both in backticks. */
    private static final Pattern RULE_ROW = Pattern.compile("^\\|\\s*`([^`]+)`\\s*\\|\\s*`([^`]+)`\\s*\\|");

    @Test
    void catalogTableListsExactlyThePublishedIds() throws IOException {
        Map<String, String> documented = documentedRows();
        Set<String> published = PublishedRules.idsOf(EveryPublishedGroup.class);

        assertEquals(published, documented.keySet(),
                "docs/rules.md disagrees with the published groups. Every published id needs a row in"
                        + " the catalog table, and every row needs a published id — add the missing"
                        + " row, or remove the stale one.");
    }

    @Test
    void catalogTableNamesTheGroupEachRuleShipsIn() throws IOException {
        Map<String, String> documented = documentedRows();

        for (ArchTests member : PublishedRules.archTestsFieldsOf(EveryPublishedGroup.class)) {
            Class<?> group = member.getDefinitionLocation();
            for (String id : PublishedRules.idsOf(group)) {
                assertEquals(group.getSimpleName(), documented.get(id),
                        () -> "docs/rules.md lists " + id + " under group '" + documented.get(id)
                                + "' but it is published from " + group.getSimpleName());
            }
        }
    }

    private static Map<String, String> documentedRows() throws IOException {
        Path rulesDoc = CANDIDATE_LOCATIONS.stream()
                .filter(Files::exists)
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "docs/rules.md not found at " + CANDIDATE_LOCATIONS
                                + "; run this test from the corral-rules module or the reactor root"));

        Map<String, String> rows = new LinkedHashMap<>();
        for (String line : Files.readAllLines(rulesDoc)) {
            Matcher row = RULE_ROW.matcher(line);
            if (row.find()) {
                String id = row.group(1);
                assertFalse(rows.containsKey(id), () -> id + " appears twice in docs/rules.md");
                rows.put(id, row.group(2));
            }
        }
        assertTrue(!rows.isEmpty(),
                "no catalog rows parsed from " + rulesDoc + "; the table format changed, or the"
                        + " file is empty, and either way these checks would pass vacuously");
        return rows;
    }
}
