package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures;

import java.util.Optional;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** MUST FLAG: the constrained type is an {@code Optional}'s content. */
@ConfigurationProperties("fallback")
public class FallbackOptionalProperties {
    private Optional<Datasource> fallback = Optional.empty();
}
