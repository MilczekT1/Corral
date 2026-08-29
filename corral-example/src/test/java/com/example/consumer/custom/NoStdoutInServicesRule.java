package com.example.consumer.custom;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import io.github.milczekt1.corral.DocumentedRule;
import io.github.milczekt1.corral.doc.RuleDoc;
import java.io.PrintStream;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * A rule this project owns, written with the library's own machinery.
 *
 * <p>A {@link RuleDoc} and {@link DocumentedRule} give it what a built-in rule has: WHY / HOW TO FIX
 * on failure, and existing violations recorded as debt.
 *
 * <p>{@code howNotToFix} is this rule's own anti-fix guidance; the global {@code AntiFixPolicy}
 * prints below it.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NoStdoutInServicesRule implements DocumentedRule {

    static final RuleDoc DOC = RuleDoc.builder()
            .id("acme.no-stdout-in-services")
            .why("""
                    Writing to stdout from a service bypasses the logging setup entirely: no level, no \
                    correlation id, no way to turn it off in production or capture it in tests.""")
            .howToFix("""
                    Inject a logger and log at the level the message deserves, or return the value and \
                    let the caller decide how to present it.""")
            .howNotToFix("""
                    Do NOT move the call into a helper class outside ..service.. to dodge the package \
                    matcher — the output still lands on stdout. Do NOT swap System.out for a logger you \
                    then silence in test configuration.""")
            .build();

    static final ArchRule DEFINITION = noClasses()
            .that().resideInAPackage("..service..")
            .should().callMethod(PrintStream.class, "println", String.class);

    @ArchTest
    public static final ArchRule rule = new NoStdoutInServicesRule().guard();

    @Override
    public ArchRule definition() {
        return DEFINITION;
    }

    @Override
    public RuleDoc doc() {
        return DOC;
    }
}
