package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** MUST IGNORE: Bean Validation ignores static fields and getters. */
@ConfigurationProperties("constants")
public class ConstantsProperties {
    @NotBlank
    private static String defaultRegion = "eu";

    @NotNull
    public static String getDefaultRegion() {
        return defaultRegion;
    }
}
