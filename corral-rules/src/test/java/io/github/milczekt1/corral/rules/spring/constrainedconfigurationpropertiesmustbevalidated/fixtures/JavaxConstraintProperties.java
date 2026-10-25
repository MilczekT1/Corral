package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** MUST FLAG: the only constraint is a project annotation meta-annotated {@code javax} {@code @Constraint}. */
@ConfigurationProperties("legacy-code")
public class JavaxConstraintProperties {
    @LegacyCode
    private String code;
}
