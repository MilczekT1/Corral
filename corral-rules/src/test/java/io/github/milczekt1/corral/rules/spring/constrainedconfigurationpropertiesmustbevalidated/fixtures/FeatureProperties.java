package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** MUST IGNORE: no constraints. */
@ConfigurationProperties("feature")
public class FeatureProperties {
    private boolean enabled;
}
