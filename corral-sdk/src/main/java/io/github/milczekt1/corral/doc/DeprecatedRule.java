package io.github.milczekt1.corral.doc;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.archunit.lang.Priority;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.experimental.UtilityClass;

/**
 * A retired rule id, kept alive so that retiring it is not a silent breaking change.
 *
 * <p>ArchUnit's {@code ViolationStore} SPI takes only an {@code ArchRule} and offers no rename verb,
 * so a renamed id cannot carry its recorded violations across — the rule re-seeds clean and the
 * build goes green with nothing enforced. Corral therefore never renames. The old id stays
 * registered as a rule that always passes and names its replacement, so a consumer excluding it
 * still resolves and a consumer reading the catalog is told where the rule went.
 *
 * <p>Wire it exactly like a live rule, so consumers keep evaluating it:
 * <pre>{@code
 * @ArchTest
 * public static final ArchRule oldName = DeprecatedRule.supersededBy(
 *         "test.class-naming-convention", "test.class-names-must-end-with-test-or-it",
 *         "renamed to carry a polarity marker");
 * }</pre>
 */
@UtilityClass
public class DeprecatedRule {

    /**
     * Every id ever passed as {@code retiredId} to {@link #supersededBy}, this JVM's lifetime.
     *
     * <p>A retired id predates whatever grammar the catalog enforces today — it was legal under an
     * older or no convention at all, and {@code supersededBy}'s whole point is that it can never be
     * renamed to comply. {@code RuleIdGrammarTest} needs to tell "retired" from "wrong" before it
     * fails an id, and this set is how: an id here is exempted, not grandfathered by accident.
     *
     * <p>{@code ConcurrentHashMap}-backed, mirroring {@link RuleRegistry}'s {@code DOCS} map — Surefire
     * reuses one JVM across test classes, so registration and lookup can race across them.
     */
    private static final Set<String> RETIRED_IDS = ConcurrentHashMap.newKeySet();

    /**
     * Registers {@code retiredId} pointing at {@code replacementId} and returns a rule that always
     * passes.
     *
     * <p>Deliberately never frozen: freezing would write an index entry claiming the retired id is
     * enforced. It is not — it is a signpost.
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
        return new Retired(retiredId);
    }

    /**
     * Every id retired so far, for a grammar check to exempt. See {@link #RETIRED_IDS}.
     */
    public static Set<String> retiredIds() {
        return Collections.unmodifiableSet(RETIRED_IDS);
    }

    private static final class Retired implements ArchRule {

        private final String description;

        private Retired(String description) {
            this.description = description;
        }

        @Override
        public void check(JavaClasses classes) {
            // A signpost, not a check.
        }

        @Override
        public EvaluationResult evaluate(JavaClasses classes) {
            // No ConditionEvents are ever added, so this evaluation never carries a violation.
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
