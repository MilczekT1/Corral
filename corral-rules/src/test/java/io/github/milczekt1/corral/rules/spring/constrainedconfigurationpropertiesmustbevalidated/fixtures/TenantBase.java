package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** MUST IGNORE: bound but unconstrained, and abstract. */
@ConfigurationProperties("tenant")
public abstract class TenantBase {
    private String name;
}
