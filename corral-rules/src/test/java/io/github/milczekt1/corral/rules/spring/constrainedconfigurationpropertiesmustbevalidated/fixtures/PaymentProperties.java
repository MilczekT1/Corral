package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** MUST FLAG: constrained, bound, and not {@code @Validated}; {@code region} carries no constraint. */
@ConfigurationProperties("payments")
public class PaymentProperties {
    @NotBlank
    private String apiKey;
    @Min(1)
    private int retries;
    private String region;
}
