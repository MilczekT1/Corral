package io.github.milczekt1.corral.groups;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.junit.ArchTests;
import io.github.milczekt1.corral.reflect.PublishedRules;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * Keeps {@code docs/rules.md} in step with what the catalog ships: every id in a table exists, every
 * shipped id is in a table, and each row names how the rule is reached — the group it ships in, or
 * the class a consumer wires directly.
 *
 * <p>The tables are the consumer-facing catalog and the only place a rule is described in prose, so
 * drift there is the most visible form of catalog rot — and the one a reviewer is least likely to
 * spot in a diff that touches Java.
 */
class RulesCatalogDocTest {

    /** Resolved from the module directory, where Surefire runs; the reactor root is the fallback. */
    private static final List<Path> CANDIDATE_LOCATIONS = List.of(
            Path.of("..", "docs", "rules.md"),
            Path.of("docs", "rules.md"));

    /** A catalog row: the id in the first cell and the group or wiring in the second, both in backticks. */
    private static final Pattern RULE_ROW = Pattern.compile("^\\|\\s*`([^`]+)`\\s*\\|\\s*`([^`]+)`\\s*\\|");

    /** The heading the standalone table sits under; the grouped table sits above every heading. */
    private static final String STANDALONE_HEADING = "## Standalone rules";

    private static final String GROUPED_SECTION = "";

    @Test
    void catalogTableListsExactlyTheIdsPublishedFromGroups() throws IOException {
        Set<String> published = PublishedRules.idsOf(EveryPublishedGroup.class);

        assertEquals(published, rowsUnder(GROUPED_SECTION).keySet(),
                "docs/rules.md disagrees with the published groups. Every published id needs a row in"
                        + " the catalog table, and every row needs a published id — add the missing"
                        + " row, or remove the stale one.");
    }

    @Test
    void catalogTableNamesTheGroupEachRuleShipsIn() throws IOException {
        Map<String, String> documented = rowsUnder(GROUPED_SECTION);

        for (ArchTests member : PublishedRules.archTestsFieldsOf(EveryPublishedGroup.class)) {
            Class<?> group = member.getDefinitionLocation();
            for (String id : PublishedRules.idsOf(group)) {
                assertEquals(group.getSimpleName(), documented.get(id),
                        () -> "docs/rules.md lists " + id + " under group '" + documented.get(id)
                                + "' but it is published from " + group.getSimpleName());
            }
        }
    }

    @Test
    void standaloneTableListsExactlyTheRulesThatShipOutsideAGroup() throws IOException {
        Set<String> standalone = PublishedRules.idsOf(EveryStandaloneRule.class);

        assertEquals(standalone, rowsUnder(STANDALONE_HEADING).keySet(),
                "docs/rules.md disagrees with the standalone rules. A rule in no group is only ever found"
                        + " by reading this table, so the row is the whole of its discoverability.");
    }

    /** The cell is the line a consumer copies, so it has to name the class they wire. */
    @Test
    void standaloneTableNamesTheRuleClassToWire() throws IOException {
        Map<String, String> documented = rowsUnder(STANDALONE_HEADING);

        for (ArchTests member : PublishedRules.archTestsFieldsOf(EveryStandaloneRule.class)) {
            Class<?> ruleClass = member.getDefinitionLocation();
            for (String id : PublishedRules.idsOf(ruleClass)) {
                assertTrue(documented.getOrDefault(id, "").contains(ruleClass.getSimpleName()),
                        () -> "docs/rules.md tells consumers to wire " + id + " with '"
                                + documented.get(id) + "', which does not name "
                                + ruleClass.getSimpleName());
            }
        }
    }

    /**
     * The rows of one table, keyed by id. Sections are delimited by {@code ##} headings, and the
     * grouped table sits above the first of them.
     */
    private static Map<String, String> rowsUnder(String heading) throws IOException {
        Path rulesDoc = CANDIDATE_LOCATIONS.stream()
                .filter(Files::exists)
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "docs/rules.md not found at " + CANDIDATE_LOCATIONS
                                + "; run this test from the corral-rules module or the reactor root"));

        Map<String, String> rows = new LinkedHashMap<>();
        Set<String> everyId = new HashSet<>();
        String section = GROUPED_SECTION;
        for (String line : Files.readAllLines(rulesDoc)) {
            if (line.startsWith("## ")) {
                section = line.strip();
                continue;
            }
            Matcher row = RULE_ROW.matcher(line);
            if (!row.find()) {
                continue;
            }
            String id = row.group(1);
            assertTrue(everyId.add(id), () -> id + " appears twice in docs/rules.md");
            if (section.equals(heading)) {
                rows.put(id, row.group(2));
            }
        }
        assertFalse(rows.isEmpty(),
                "no rows parsed from " + rulesDoc + " under '" + heading + "'; the table format"
                        + " changed, or the section is gone, and either way these checks would pass"
                        + " vacuously");
        return rows;
    }
}
