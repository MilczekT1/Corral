package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** MUST IGNORE: constrained methods that are not getters. */
@ConfigurationProperties("resolver")
public class ResolverProperties {
    private String endpoint = "localhost";

    @NotNull
    public String resolve() {
        return endpoint;
    }

    @NotNull
    public String issuer() {
        return endpoint;
    }

    @NotNull
    public String getEndpoint(String region) {
        return region + "." + endpoint;
    }
}
