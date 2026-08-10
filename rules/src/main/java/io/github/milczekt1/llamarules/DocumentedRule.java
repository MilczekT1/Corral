package io.github.milczekt1.llamarules;

import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
import io.github.milczekt1.llamarules.doc.RuleDoc;
import io.github.milczekt1.llamarules.doc.RuleRegistry;

/**
 * The contract every rule class implements, in this library and in consumers.
 *
 * <p>Pairing is the point: {@link #guard()} reads the doc and the raw rule off the same object,
 * so a rule cannot be frozen under another rule's documentation.
 *
 * <pre>{@code
 * public final class NoStdoutInServicesRule implements DocumentedRule {
 *
 *     static final ArchRule RULE = noClasses()...;
 *     static final RuleDoc DOC = RuleDoc.builder()...build();
 *
 *     @Override public ArchRule definition() { return RULE; }
 *     @Override public RuleDoc doc() { return DOC; }
 *
 *     @ArchTest
 *     public static final ArchRule rule = new NoStdoutInServicesRule().guard();
 * }
 * }</pre>
 *
 * <p><strong>Declare the {@code @ArchTest} field last.</strong> It runs {@code guard()} during
 * class initialisation, which reads the constants above it; a constant declared below is still
 * {@code null} at that point. Nothing but ordering prevents that — an interface cannot mandate a
 * static field, so ArchUnit's discovery of the field remains the author's responsibility.
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
     */
    default ArchRule guard() {
        RuleRegistry.register(doc());
        return FreezingArchRule.freeze(definition().as(doc().id()).allowEmptyShould(true));
    }
}
