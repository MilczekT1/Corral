package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** FLAGGED: the only constraint is a project annotation meta-annotated with {@code javax} {@code @Constraint}. */
@ConfigurationProperties("legacy-code")
public class LegacyCodeProperties {
    @LegacyCode
    private String code;
}
