package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** MUST FLAG: the constrained type is an array component. */
@ConfigurationProperties("archives")
public class ArchiveArrayProperties {
    private Datasource[] archives = {};
}
