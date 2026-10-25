package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** MUST IGNORE: validated through a composed annotation. */
@ValidatedConfiguration
@ConfigurationProperties("rate-limit")
public class RateLimitProperties {
    @Min(1)
    private int permits;
}
