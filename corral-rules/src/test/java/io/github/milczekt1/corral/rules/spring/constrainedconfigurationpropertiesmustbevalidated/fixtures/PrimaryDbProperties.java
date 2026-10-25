package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** MUST FLAG: no constraint of its own; the {@link Datasource} it holds has one. */
@ConfigurationProperties("primary-db")
public class PrimaryDbProperties {
    private Datasource db = new Datasource();
}
