package io.github.milczekt1.corral.rules.spring.novalueonfield.fixtures;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** MUST FLAG both fields: Spring skips static fields, so each keeps its initial value for good. */
@Component
public class CacheSettings {

    @Value("${cache.region}")
    private static String region;

    @Value("${cache.max-entries}")
    private static final int MAX_ENTRIES = 100;

    public String describe() {
        return region + ":" + MAX_ENTRIES;
    }
}
