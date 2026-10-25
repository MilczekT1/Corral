package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures;

import jakarta.validation.constraints.NotBlank;

/** MUST IGNORE: bound only through a {@code @Validated} binding method. */
public class GatewaySettings {
    @NotBlank
    private String url;
}
