package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures;

import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** MUST FLAG: a repeated {@code @Pattern} compiles to {@code Pattern$List}. */
@ConfigurationProperties("dns")
public class HostnameProperties {
    @Pattern(regexp = "[a-z.]+")
    @Pattern(regexp = ".{1,63}")
    private String hostname;
}
