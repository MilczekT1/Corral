package io.github.milczekt1.archrules.groups;

import org.junit.jupiter.api.Test;

class TestingRulesTest {

    @Test
    void membersMatchArchTestsFields() {
        GroupMembership.assertMembersMatchArchTestsFields(TestingRules.class, TestingRules.members());
    }
}
