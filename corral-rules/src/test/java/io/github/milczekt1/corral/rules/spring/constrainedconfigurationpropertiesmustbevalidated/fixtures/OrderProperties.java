package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** MUST IGNORE: validated. */
@Validated
@ConfigurationProperties("orders")
public class OrderProperties {
    @NotBlank
    private String queue;
}
