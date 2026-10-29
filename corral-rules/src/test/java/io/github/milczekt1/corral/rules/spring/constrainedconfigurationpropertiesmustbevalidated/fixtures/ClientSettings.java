package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;

/** NOT FLAGGED as a class: not annotated {@code @ConfigurationProperties}; its binding methods are checked instead. */
public class ClientSettings {
    @NotNull
    private Duration timeout;
}
