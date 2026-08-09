package io.github.milczekt1.llamarules.groups;

import org.junit.jupiter.api.Test;

/**
 * The single guard on the group contract, for every group in the tree.
 *
 * <p>A group states its membership twice — in {@code members()} (what the completeness and README
 * tooling reads) and as {@code @ArchTest ArchTests} fields (the only members
 * {@code ArchTests.in(...)} descends into, i.e. what consumers actually evaluate). Diverge them and
 * the build stays green while nobody ever runs the missing group's rules.
 *
 * <p>This test walks down from {@link AllCentralRules} instead of naming groups one by one. A
 * per-group test is opt-in, so coverage decays exactly as the rule count grows: add a group, forget
 * its test, and its pair is unguarded — the very failure mode the contract exists to prevent. Here
 * a new group is guarded as soon as it is reachable, and it has to be reachable to do anything at
 * all. There is no way to add one that escapes.
 */
class GroupMembershipTest {

    @Test
    void everyGroupInTheTreeDeclaresItsMembersBothWays() {
        GroupMembership.assertEveryGroupReachableFrom(AllCentralRules.class);
    }
}
