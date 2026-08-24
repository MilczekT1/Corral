package io.github.milczekt1.corral.exclude;

import io.github.milczekt1.corral.doc.RuleDoc;

/**
 * One line of {@code corral-exclusions.txt}: a rule this codebase permanently opts out of, and why.
 *
 * <p>The record is the invariant — an {@code Exclusion} that exists is well formed. {@link #ruleId()}
 * has the shape of a {@link RuleDoc} id, and {@link #reason()} is non-blank. Corral cannot judge
 * whether a reason is a good one; it can only make its absence fatal.
 *
 * @param ruleId the id of the rule to exclude, in {@link RuleDoc} id shape
 * @param reason why the rule does not apply to this codebase; never blank
 */
public record Exclusion(String ruleId, String reason) {

    public Exclusion {
        if (!RuleDoc.isId(ruleId)) {
            throw new IllegalArgumentException(
                    "'" + ruleId + "' is not the shape of a rule id — an exclusion names a rule by its"
                            + " id, which is also its freeze-store key");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException(
                    "excluding '" + ruleId + "' needs a reason after '::' — a rule dropped without one"
                            + " is indistinguishable from a rule silenced");
        }
        reason = reason.strip();
    }
}
