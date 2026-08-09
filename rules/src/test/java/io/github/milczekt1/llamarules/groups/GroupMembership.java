package io.github.milczekt1.llamarules.groups;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import com.tngtech.archunit.junit.ArchTests;
import io.github.milczekt1.llamarules.testsupport.PublishedRules;
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
 * Asserts the group contract: each member is declared twice — in {@code members()} for tooling, and
 * as an {@code @ArchTest ArchTests} field for the engine. A member present only in {@code members()}
 * is never evaluated by any consumer, silently.
 *
 * <p>{@link #assertEveryGroupReachableFrom(Class)} covers the whole tree in one call, so a new group
 * is guarded the moment it becomes reachable. Per-group call sites would be opt-in, and the one
 * somebody forgets is the one that goes unenforced.
 */
final class GroupMembership {

    /**
     * Checks {@code root} and every group below it, at any depth.
     *
     * <p>A node is a group when it declares {@code @ArchTest ArchTests} fields; a rule class
     * declares {@code ArchRule} fields and is a leaf. Groups must expose a static {@code members()}
     * — without one, membership is stated once and nothing can cross-check it.
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
     * {@code root} plus everything reachable through {@code @ArchTest ArchTests} fields.
     *
     * <p>Keyed on those fields, not {@code members()}, so the walk sees exactly the tree consumers
     * evaluate. A group listed only in a parent's {@code members()} is unreachable here, and is
     * caught by that parent's own assertion.
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
