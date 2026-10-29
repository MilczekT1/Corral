package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** NOT FLAGGED: every constraint is on a member Bean Validation does not read. */
@ConfigurationProperties("ignored")
public class IgnoredMembersProperties {
    /** Static field. */
    @NotBlank
    private static String defaultRegion = "eu";

    private String endpoint = "localhost";

    /** An annotation carrying a constraint without being one. */
    @Trimmed
    private String label;

    /** Static getter. */
    @NotNull
    public static String getDefaultRegion() {
        return defaultRegion;
    }

    /** Takes a parameter, so not a getter. */
    @NotNull
    public String getEndpoint(String region) {
        return region + "." + endpoint;
    }

    /** No upper-case letter after {@code is}, so not a getter. */
    @NotNull
    public String issuer() {
        return endpoint;
    }
}
