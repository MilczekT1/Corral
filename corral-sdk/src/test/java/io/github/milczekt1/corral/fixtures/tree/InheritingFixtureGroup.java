package io.github.milczekt1.corral.fixtures.tree;

/**
 * A group that declares no member of its own: everything it publishes is inherited, one member from
 * a superclass and one from an interface.
 *
 * <p>The point is the gap this shape used to fall into. ArchUnit resolves members over all
 * supertypes, so it evaluates all three rules reachable from here; a walk built on
 * {@code getDeclaredFields()} sees zero. That mismatch is a silent under-report — every completeness
 * assertion keyed on the walk would quietly stop covering these rules.
 */
public final class InheritingFixtureGroup extends InheritedMembersBaseGroup
        implements InterfaceDeclaredRules {
}
