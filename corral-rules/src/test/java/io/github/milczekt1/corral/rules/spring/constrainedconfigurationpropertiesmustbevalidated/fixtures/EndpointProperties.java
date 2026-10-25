package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** MUST IGNORE: a constructor parameter constraint, which Boot does not check even with {@code @Validated}. */
@ConfigurationProperties("endpoint")
public class EndpointProperties {
    private final String host;

    public EndpointProperties(@NotBlank String host) {
        this.host = host;
    }
}
