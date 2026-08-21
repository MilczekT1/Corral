package io.github.milczekt1.llamaguard.fixtures.tree;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Fields that must NOT be picked up, alongside one that must.
 *
 * <p>Raw {@code ArchRule}s rather than {@code DocumentedRule}s: nothing here should reach the
 * process-wide {@code RuleRegistry}, and the walk never consults it anyway.
 *
 * <p>Not a {@code @UtilityClass} — one field is deliberately non-static, which Lombok would rewrite.
 */
public final class DecoyFieldsGroup {

    @ArchTest
    public static final ArchRule published = noClasses()
            .should().accessField(System.class, "in").as("fixture.decoy-published");

    /** No {@code @ArchTest}: declared, but not published. */
    public static final ArchRule notAnnotated = noClasses()
            .should().accessField(System.class, "in").as("fixture.decoy-unannotated");

    /**
     * Deliberately out of scope. ArchUnit does treat this as a member — it instantiates the owner
     * to read a non-static {@code @ArchTest} field — but this walk constructs nothing, so it skips
     * it. Static state is what the walk reads; running a constructor to discover a rule is a cost
     * and a side-effect risk the walk does not take on.
     */
    @ArchTest
    public final ArchRule instanceField = noClasses()
            .should().accessField(System.class, "in").as("fixture.decoy-instance");
}
