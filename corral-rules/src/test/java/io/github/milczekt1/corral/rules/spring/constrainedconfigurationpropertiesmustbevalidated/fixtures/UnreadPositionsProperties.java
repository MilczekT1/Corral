package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** MUST IGNORE: every constraint sits where Bean Validation does not read a properties object, or is not one. */
@ConfigurationProperties("unread")
public class UnreadPositionsProperties {
    @NotBlank
    private static String defaultRegion = "eu";

    private final String host;
    private String proxy;
    private List<@NotBlank String> tags = List.of();
    @Trimmed
    private String label;

    public UnreadPositionsProperties(@NotBlank String host) {
        this.host = host;
    }

    public void setProxy(@NotBlank String proxy) {
        this.proxy = proxy;
    }

    @NotNull
    public static String getDefaultRegion() {
        return defaultRegion;
    }

    @NotNull
    public String resolve() {
        return host;
    }

    @NotNull
    public String issuer() {
        return host;
    }

    @NotNull
    public String getEndpoint(String region) {
        return region + "." + host;
    }
}
