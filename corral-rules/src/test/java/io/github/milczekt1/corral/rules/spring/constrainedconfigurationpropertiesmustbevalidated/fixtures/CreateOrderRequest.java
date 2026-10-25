package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures;

import jakarta.validation.constraints.NotBlank;

/** MUST IGNORE: constrained but not bound by {@code @ConfigurationProperties}. */
public class CreateOrderRequest {
    @NotBlank
    private String sku;
}
