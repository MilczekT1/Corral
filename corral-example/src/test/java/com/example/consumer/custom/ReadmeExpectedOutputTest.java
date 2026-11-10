package com.example.consumer.custom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.consumer.billing.InvoiceService;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import io.github.milczekt1.corral.settings.CorralSettings;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReadmeExpectedOutputTest {

    private static final List<Path> CANDIDATE_LOCATIONS = List.of(
            Path.of("README.md"),
            Path.of("corral-example", "README.md"));

    private static final String SECTION = "\n## Expected output\n";

    /** The URL of the settings file in effect is machine-specific, so the README shows this instead. */
    private static final String SHOWN_SOURCE = "file:/…/corral-example/target/test-classes/corral.properties";

    @Test
    void readmeShowsTheFailureTheFormatterRenders() throws IOException {
        JavaClasses misplacedService = new ClassFileImporter().importClasses(InvoiceService.class);

        String rendered = ServicesMustLiveInServicePackageRule.DEFINITION
                .as(ServicesMustLiveInServicePackageRule.DOC.id())
                .evaluate(misplacedService)
                .getFailureReport()
                .toString()
                .replace(CorralSettings.current().source(), SHOWN_SOURCE)
                .replace("\r\n", "\n")
                .strip();

        assertEquals(documentedFailure(), rendered,
                () -> "README.md § Expected output is stale. Replace its text block with:\n" + rendered);
    }

    private static String documentedFailure() throws IOException {
        Path readme = CANDIDATE_LOCATIONS.stream().filter(Files::isRegularFile).findFirst()
                .orElseThrow(() -> new AssertionError("README.md not found from " + Path.of("").toAbsolutePath()));
        String text = Files.readString(readme).replace("\r\n", "\n");
        int section = text.indexOf(SECTION);
        assertTrue(section >= 0, () -> readme + " has no '" + SECTION.strip() + "' section");
        int open = text.indexOf("```text\n", section) + "```text\n".length();
        int close = text.indexOf("\n```", open);
        return text.substring(open, close).strip();
    }
}
