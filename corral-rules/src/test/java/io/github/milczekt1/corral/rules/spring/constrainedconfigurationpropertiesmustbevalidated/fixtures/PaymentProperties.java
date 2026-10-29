package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** FLAGGED: a {@code jakarta} constraint on a field; {@code region} carries none. */
@ConfigurationProperties("payments")
public class PaymentProperties {
    @NotBlank
    private String apiKey;
    private String region;
}
