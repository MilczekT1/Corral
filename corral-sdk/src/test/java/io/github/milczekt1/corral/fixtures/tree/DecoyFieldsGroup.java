package io.github.milczekt1.corral.fixtures.tree;

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
     * Out of scope: ArchUnit instantiates the owner to read a non-static {@code @ArchTest} field,
     * while the walk constructs nothing and reads static state only.
     */
    @ArchTest
    public final ArchRule instanceField = noClasses()
            .should().accessField(System.class, "in").as("fixture.decoy-instance");
}
