package io.github.milczekt1.corral;

import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
import io.github.milczekt1.corral.doc.RuleDoc;
import io.github.milczekt1.corral.doc.RuleRegistry;
import io.github.milczekt1.corral.guard.IgnorePatternsGuard;

/**
 * The contract every rule class implements, in this library and in consumers.
 *
 * <p>Pairing is the point: {@link #guard()} reads the doc and the raw rule off the same object,
 * so a rule cannot be frozen under another rule's documentation.
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
 * {@code guard()} during class initialisation, and a static field declared after it is still
 * {@code null} at that point. Method order is irrelevant — only fields initialise. Nothing but that
 * ordering prevents the mistake, and an interface cannot mandate a static field at all, so ArchUnit
 * discovering the field remains the author's responsibility.
 */
public interface DocumentedRule {

    /** The raw, unfrozen rule. Test <em>this</em>: {@link #guard()} seeds and passes. */
    ArchRule definition();

    RuleDoc doc();

    /**
     * The rule consumers evaluate. Registers the doc so the failure formatter can find the prose, pins
     * the description to the doc id — ArchUnit derives the freeze-store key from the description, so
     * this is what stops a reworded sentence re-seeding every consumer's store — allows an empty
     * {@code should} so a module with no matching classes stays green, and freezes, so that adopting
     * a rule records existing debt instead of blocking in-flight work.
     *
     * <p>Also the hook for {@link IgnorePatternsGuard}: every rule class runs this, in every consumer,
     * with nothing to configure, so it is the one place a whole-catalog kill switch cannot hide from.
     */
    default ArchRule guard() {
        RuleRegistry.register(doc());
        return IgnorePatternsGuard.interposeOn(
                FreezingArchRule.freeze(definition().as(doc().id()).allowEmptyShould(true)));
    }
}
