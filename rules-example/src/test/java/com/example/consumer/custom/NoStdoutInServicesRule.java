package com.example.consumer.custom;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import io.github.milczekt1.llamarules.freeze.Freezer;
import io.github.milczekt1.llamarules.RuleDoc;
import java.io.PrintStream;

/**
 * A rule this project owns, written with the library's own machinery.
 *
 * <p>Any {@code ArchRule} works with {@code @ArchTest}, but a plain one renders through ArchUnit's
 * default format — one line, no guidance. Giving it a {@link RuleDoc} and freezing it through
 * {@link Freezer} makes it behave exactly like a built-in rule: WHY / HOW TO FIX on failure,
 * existing violations recorded as debt rather than blocking.
 *
 * <p>{@code howNotToFix} is where this rule's own anti-fix guidance goes. The global policy in
 * {@code AntiFixPolicy} is identical for every rule and prints below it.
 */
public final class NoStdoutInServicesRule {

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

    static final ArchRule RULE = noClasses()
            .that().resideInAPackage("..service..")
            .should().callMethod(PrintStream.class, "println", String.class);

    @ArchTest
    public static final ArchRule rule = Freezer.freeze(RULE, DOC);

    private NoStdoutInServicesRule() {
    }
}
