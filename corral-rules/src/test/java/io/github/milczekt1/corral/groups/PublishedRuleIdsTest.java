package io.github.milczekt1.corral.groups;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.milczekt1.corral.reflect.PublishedRules;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Pins the published id set to a committed file.
 *
 * <p>A rule id is the freeze-store key. Renaming or removing one orphans every consumer's recorded
 * violations: the rule re-seeds clean and the build goes green with nothing enforced. Nothing about
 * that is visible in a normal diff — the rule class still looks fine. This file makes it visible,
 * as a -old/+new pair a reviewer has to acknowledge.
 *
 * <p>When this test fails, do not regenerate the file to make it pass. Ask whether the id changed on
 * purpose, and if it did, whether the old one needs deprecating instead.
 */
class PublishedRuleIdsTest {

    private static final String GOLDEN_FILE = "published-rule-ids.txt";

    @Test
    void thePublishedIdSetMatchesTheCommittedList() throws IOException {
        List<String> published = PublishedRules.idsOf(AllCentralRules.class).stream().sorted().toList();

        assertEquals(committedIds(), published,
                "the published rule ids no longer match " + GOLDEN_FILE + ". A rule id is a"
                        + " freeze-store key: renaming one silently orphans every consumer's recorded"
                        + " violations, so the rule stops enforcing and the build stays green."
                        + " Deprecate the old id rather than renaming it; only then update this file.");
    }

    private static List<String> committedIds() throws IOException {
        try (InputStream in = PublishedRuleIdsTest.class.getClassLoader()
                .getResourceAsStream(GOLDEN_FILE)) {
            if (in == null) {
                throw new IllegalStateException(GOLDEN_FILE + " is missing from the test classpath");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).lines()
                    .map(String::strip)
                    .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .sorted()
                    .toList();
        }
    }
}
