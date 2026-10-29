package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** NOT FLAGGED where it compiled, in test output; FLAGGED once copied into a production layout. */
@ConfigurationProperties("test-output")
public class TestOutputProperties {
    @NotBlank
    private String fixture;
}
