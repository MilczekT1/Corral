package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** MUST IGNORE: Boot finds {@code @Validated} on the superclass. */
@ConfigurationProperties("cache")
public class CacheProperties extends ValidatedBaseProperties {
    @Positive
    private int size;
}
