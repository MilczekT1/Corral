package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** MUST FLAG: Boot validates the {@code @Validated} binding method, but the class shape still flags. */
@ConfigurationProperties("queue")
public class QueueProperties {
    @NotBlank
    private String name;
}
