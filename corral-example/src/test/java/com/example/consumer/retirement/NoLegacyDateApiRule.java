package com.example.consumer.retirement;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import io.github.milczekt1.corral.DocumentedRule;
import io.github.milczekt1.corral.doc.RuleDoc;
import java.util.Calendar;
import java.util.Date;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * The live rule behind this package's retirement demo. It shipped as {@code acme.no-java-util-date}
 * and grew to cover {@link Calendar} as well, so the old slug stopped describing it — see
 * {@link DateRulesGroup} for the retired id that now points here.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NoLegacyDateApiRule implements DocumentedRule {

    static final RuleDoc DOC = RuleDoc.builder()
            .id("acme.no-legacy-date-api")
            .why("""
                    java.util.Date and java.util.Calendar are mutable, are not thread-safe, and carry \
                    an implicit default time zone, so the same code gives different answers on a \
                    developer laptop and on a server.""")
            .howToFix("""
                    Use java.time: Instant for a moment, LocalDate for a calendar date, ZonedDateTime \
                    when the zone is part of the value. Convert at the boundary with Date.toInstant() \
                    if a framework still hands you one.""")
            .howNotToFix("""
                    Do NOT wrap the call in a helper outside the scanned packages — the mutable value \
                    still crosses your code. Do NOT set a fixed default time zone at startup to make \
                    the ambiguity invisible.""")
            .build();

    /**
     * Scoped to {@code ..service..} rather than left unscoped: a rule class naming the types it
     * forbids matches itself, and freezing that would record the rule's own definition as debt.
     */
    static final ArchRule DEFINITION = noClasses()
            .that().resideInAPackage("..service..")
            .should().dependOnClassesThat().belongToAnyOf(Date.class, Calendar.class);

    @ArchTest
    public static final ArchRule rule = new NoLegacyDateApiRule().guard();

    @Override
    public ArchRule definition() {
        return DEFINITION;
    }

    @Override
    public RuleDoc doc() {
        return DOC;
    }
}
