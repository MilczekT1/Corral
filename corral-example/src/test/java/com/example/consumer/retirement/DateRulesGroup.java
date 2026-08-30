package com.example.consumer.retirement;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;
import com.tngtech.archunit.lang.ArchRule;
import io.github.milczekt1.corral.doc.DeprecatedRule;

/**
 * The group that published {@code acme.no-java-util-date}, and still does — as a signpost.
 *
 * <p>An id is a freeze-store key, and ArchUnit's {@code ViolationStore} SPI has no rename verb, so
 * renaming one leaves every consumer's recorded violations filed under the old key: the rule
 * re-seeds clean and the build goes green enforcing nothing. Retiring is the safe move, and the
 * retired id is wired here exactly like a live rule so consumers keep evaluating it.
 *
 * <p>The signpost always passes and is never frozen — freezing it would claim it is enforced. It
 * costs no {@code stored.rules} entry, and an exclusion still naming the old id keeps resolving
 * against it instead of warning that it matched nothing.
 */
final class DateRulesGroup {

    @ArchTest
    static final ArchTests noLegacyDateApi = ArchTests.in(NoLegacyDateApiRule.class);

    @ArchTest
    static final ArchRule noJavaUtilDate = DeprecatedRule.supersededBy(
            "acme.no-java-util-date",
            "acme.no-legacy-date-api",
            "renamed when it grew to cover Calendar as well as Date, so the old slug no longer"
                    + " described what it checks");

    private DateRulesGroup() {
    }
}
