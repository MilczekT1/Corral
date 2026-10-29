package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** NOT FLAGGED: inherits {@code @Validated} through a composed annotation on its superclass. */
@ConfigurationProperties("cache")
public class CacheProperties extends ValidatedSettings {
    @Positive
    private int size;
}
