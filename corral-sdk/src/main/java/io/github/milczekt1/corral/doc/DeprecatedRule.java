package io.github.milczekt1.corral.doc;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.archunit.lang.Priority;
import io.github.milczekt1.corral.exclude.RuleExclusions;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.experimental.UtilityClass;

/**
 * A retired rule id, registered as a rule that always passes and names its replacement.
 *
 * <p>Ids are retired rather than renamed: ArchUnit's {@code ViolationStore} SPI has no rename verb,
 * so a renamed id re-seeds clean and enforces nothing.
 *
 * <p>Wire it exactly like a live rule, so consumers keep evaluating it:
 * <pre>{@code
 * @ArchTest
 * public static final ArchRule oldName = DeprecatedRule.supersededBy(
 *         "test.class-naming-convention", "corral.test.class-names-must-end-with-test-or-it",
 *         "renamed to carry a polarity marker");
 * }</pre>
 */
@UtilityClass
public class DeprecatedRule {

    /**
     * Every id ever passed as {@code retiredId} to {@link #supersededBy}, this JVM's lifetime.
     * A retired id predates today's grammar and can never be renamed to comply, so
     * {@code RuleIdGrammarTest} exempts what is listed here.
     */
    private static final Set<String> RETIRED_IDS = ConcurrentHashMap.newKeySet();

    /**
     * Registers {@code retiredId} pointing at {@code replacementId} and returns a rule that always
     * passes. Never frozen, so no index entry claims the retired id is enforced.
     *
     * <p>Routed through {@link RuleExclusions#applyTo} so an exclusion naming {@code retiredId}
     * counts as matched. {@code retiredId} must satisfy {@link RuleDoc}'s id shape and caps.
     */
    public static ArchRule supersededBy(String retiredId, String replacementId, String why) {
        RuleRegistry.register(RuleDoc.builder()
                .id(retiredId)
                .why(why)
                .howToFix("This rule was retired. Its replacement is '" + replacementId
                        + "' — exclude or fix that id instead. Nothing is enforced under '"
                        + retiredId + "' any more, and it stays listed only so an exclusion naming"
                        + " it keeps resolving.")
                .build());
        RETIRED_IDS.add(retiredId);
        return RuleExclusions.applyTo(new Retired(retiredId), retiredId);
    }

    /** Every id retired so far, for a grammar check to exempt. */
    public static Set<String> retiredIds() {
        return Collections.unmodifiableSet(RETIRED_IDS);
    }

    private record Retired(String description) implements ArchRule {

        @Override
        public void check(JavaClasses classes) {
            // A signpost, not a check.
        }

        @Override
        public EvaluationResult evaluate(JavaClasses classes) {
            return new EvaluationResult(this, Priority.MEDIUM);
        }

        @Override
        public ArchRule because(String reason) {
            return this;
        }

        @Override
        public ArchRule allowEmptyShould(boolean allowEmptyShould) {
            return this;
        }

        @Override
        public ArchRule as(String newDescription) {
            return new Retired(newDescription);
        }

        @Override
        public String getDescription() {
            return description;
        }
    }
}
