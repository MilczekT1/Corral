package io.github.milczekt1.corral.rules.layering.notestlibrariesinproduction.fixtures;

import java.util.Map;
import org.springframework.test.util.ReflectionTestUtils;

/** MUST FLAG {@code org.springframework.test..}: spring-test's field injector used to prime a cache. */
public class CacheWarmer {

    void prime(Object cache, Map<String, Object> entries) {
        ReflectionTestUtils.setField(cache, "entries", Map.copyOf(entries));
    }
}
