package io.github.milczekt1.corral.fixtures.tree;

/**
 * A group that declares no member of its own: everything it publishes is inherited, one member from
 * a superclass and one from an interface.
 *
 * <p>ArchUnit resolves members over all supertypes and evaluates all three rules reachable from
 * here; a walk built on {@code getDeclaredFields()} alone sees zero.
 */
public final class InheritingFixtureGroup extends InheritedMembersBaseGroup
        implements InterfaceDeclaredRules {
}
