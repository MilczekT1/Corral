package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures;

import jakarta.validation.constraints.NotBlank;
import org.springframework.stereotype.Component;

/** MUST FLAG: Boot finds {@code @ConfigurationProperties} on the superclass. */
@Component
public class TenantProperties extends TenantBase {
    @NotBlank
    private String id;
}
