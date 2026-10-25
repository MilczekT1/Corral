package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;

/** MUST IGNORE as a class: bound only through {@link ClientConfiguration#clientSettings()}. */
public class ClientSettings {
    @NotNull
    private Duration timeout;
}
