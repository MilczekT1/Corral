package io.github.milczekt1.archrules.groups;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.tngtech.archunit.junit.ArchTests;
import io.github.milczekt1.archrules.testsupport.PublishedRules;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Shared assertions for the group contract: a group declares each member twice — in {@code
 * members()} for tooling, and as an {@code @ArchTest ArchTests} field for the engine. Divergence is
 * silent in the worst direction: a member present only in {@code members()} is documented and
 * completeness-checked but never evaluated by any consumer.
 */
final class GroupMembership {

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

    private GroupMembership() {
    }
}
