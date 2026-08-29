package io.github.milczekt1.corral;

import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
import io.github.milczekt1.corral.doc.RuleDoc;
import io.github.milczekt1.corral.doc.RuleRegistry;
import io.github.milczekt1.corral.exclude.RuleExclusions;
import io.github.milczekt1.corral.guard.IgnorePatternsGuard;

/**
 * The contract every rule class implements, in this library and in consumers. {@link #guard()} reads
 * the doc and the raw rule off the same object, so a rule cannot be frozen under another rule's doc.
 *
 * <pre>{@code
 * public final class NoStdoutInServicesRule implements DocumentedRule {
 *
 *     static final ArchRule DEFINITION = noClasses()...;
 *     static final RuleDoc DOC = RuleDoc.builder()...build();
 *
 *     @ArchTest
 *     public static final ArchRule rule = new NoStdoutInServicesRule().guard();
 *
 *     @Override public ArchRule definition() { return DEFINITION; }
 *     @Override public RuleDoc doc() { return DOC; }
 * }
 * }</pre>
 *
 * <p><strong>Declare the {@code @ArchTest} field below the constants it reads.</strong> It runs
 * {@code guard()} during class initialisation, so a static field declared after it is still
 * {@code null}. Nothing enforces this.
 */
public interface DocumentedRule {

    /** The raw, unfrozen rule. Test <em>this</em>: {@link #guard()} seeds and passes. */
    ArchRule definition();

    RuleDoc doc();

    /**
     * The rule consumers evaluate: doc registered, description pinned to the doc id (the freeze-store
     * key), empty {@code should} allowed, frozen.
     *
     * <p>The one place every rule class reaches, so {@link IgnorePatternsGuard} and
     * {@link RuleExclusions} hook in here.
     *
     * <p>Order matters. The exclusion wraps the frozen rule, keeping an excluded rule out of the
     * freeze store; the ignore-patterns check is outermost, so a kill switch is reported even for an
     * excluded rule.
     */
    default ArchRule guard() {
        RuleRegistry.register(doc());
        ArchRule frozen = FreezingArchRule.freeze(definition().as(doc().id()).allowEmptyShould(true));
        ArchRule enforcedHere = RuleExclusions.applyTo(frozen, doc().id());
        ArchRule warnedIfUnmatched = RuleExclusions.warnUnmatchedExclusionsOnFirstEvaluation(enforcedHere);
        return IgnorePatternsGuard.interposeOn(warnedIfUnmatched);
    }
}
