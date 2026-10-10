package io.github.milczekt1.corral.springboot3.groups;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.milczekt1.corral.reflect.PublishedRules;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SpringBoot3DeprecationsRulesGroupTest {

    @Test
    void publishesExactlyTheBoot3DeprecationIds() {
        assertEquals(Set.of("corral.test.spring.no-legacy-mockbean-and-spybean"),
                PublishedRules.idsOf(SpringBoot3DeprecationsRulesGroup.class));
    }
}
