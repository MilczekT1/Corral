package io.github.milczekt1.corral.rules.spring.configurationpropertiesmustuseprojectprefix.fixtures;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** MUST IGNORE: the {@code prefix} attribute, under the project prefix. */
@ConfigurationProperties(prefix = "corral.billing")
public class BillingProperties {
    private String currency;
}
