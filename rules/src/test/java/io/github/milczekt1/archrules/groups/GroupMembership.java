package io.github.milczekt1.archrules.groups;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import com.tngtech.archunit.junit.ArchTests;
import io.github.milczekt1.archrules.testsupport.PublishedRules;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Shared assertions for the group contract: a group declares each member twice — in {@code
 * members()} for tooling, and as an {@code @ArchTest ArchTests} field for the engine. Divergence is
 * silent in the worst direction: a member present only in {@code members()} is documented and
 * completeness-checked but never evaluated by any consumer.
 *
 * <p>{@link #assertEveryGroupReachableFrom(Class)} applies that contract to the whole tree in one
 * call. It exists because a per-group call site is opt-in: add a group, forget to write its test,
 * and the pair goes unguarded — exactly the silent non-enforcement this class is for. Walking from
 * {@link AllCentralRules} instead means a new group is guarded the moment it becomes reachable,
 * which is the same moment it starts mattering.
 */
final class GroupMembership {

    /**
     * Asserts the {@code members()}/{@code @ArchTest} contract for every group reachable from
     * {@code root}, {@code root} itself included, at any nesting depth.
     *
     * <p>A node counts as a group when it declares at least one {@code @ArchTest ArchTests} field
     * (a rule class declares {@code ArchRule} fields instead, and is a leaf here). Every such node
     * must expose a static {@code members()}; a group without one would otherwise slip through
     * having stated its membership only once, leaving nothing to cross-check.
     */
    static void assertEveryGroupReachableFrom(Class<?> root) {
        for (Class<?> node : nodesReachableFrom(root)) {
            if (PublishedRules.archTestsFieldsOf(node).isEmpty()) {
                continue; // a rule class: nothing to cross-check.
            }
            List<Class<?>> members = declaredMembersOf(node).orElseGet(() -> fail(
                    node.getSimpleName() + " declares @ArchTest ArchTests fields but no static "
                            + "members(); without it the group states its membership only once and "
                            + "nothing can cross-check it"));
            assertMembersMatchArchTestsFields(node, members);
        }
    }

    static void assertMembersMatchArchTestsFields(Class<?> group, List<Class<?>> members) {
        Set<Class<?>> exposed = PublishedRules.archTestsFieldsOf(group).stream()
                .map(ArchTests::getDefinitionLocation)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        assertEquals(Set.copyOf(members), exposed,
                group.getSimpleName() + ": members() and @ArchTest ArchTests fields have diverged; "
                        + "every member needs BOTH");
        assertEquals(members.size(), PublishedRules.archTestsFieldsOf(group).size(),
                group.getSimpleName() + ": one @ArchTest ArchTests field per member, no duplicates");
    }

    /**
     * {@code root} plus every class reachable from it through {@code @ArchTest ArchTests} fields,
     * in walk order.
     *
     * <p>Deliberately keyed on those fields rather than on {@code members()}: they are what
     * {@code ArchTests.in(...)} actually descends into, so this walk sees exactly the tree
     * consumers evaluate. A group listed only in a parent's {@code members()} is unreachable here
     * — and is caught by that parent's own assertion, which is where the divergence lives.
     */
    private static List<Class<?>> nodesReachableFrom(Class<?> root) {
        List<Class<?>> nodes = new ArrayList<>();
        nodes.add(root);
        for (ArchTests nested : PublishedRules.archTestsFieldsOf(root)) {
            nodes.addAll(nodesReachableFrom(nested.getDefinitionLocation()));
        }
        return nodes;
    }

    /** The node's own {@code members()}, or empty when it declares no usable one. */
    @SuppressWarnings("unchecked")
    private static Optional<List<Class<?>>> declaredMembersOf(Class<?> node) {
        Method members;
        try {
            members = node.getDeclaredMethod("members");
        } catch (NoSuchMethodException e) {
            return Optional.empty();
        }
        if (!Modifier.isStatic(members.getModifiers())
                || !List.class.isAssignableFrom(members.getReturnType())) {
            return Optional.empty();
        }
        try {
            return Optional.of((List<Class<?>>) members.invoke(null));
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new AssertionError("could not read " + node.getSimpleName() + ".members()", e);
        }
    }

    private GroupMembership() {
    }
}
