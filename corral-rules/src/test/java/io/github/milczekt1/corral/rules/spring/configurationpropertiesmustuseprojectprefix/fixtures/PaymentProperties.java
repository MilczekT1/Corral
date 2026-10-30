package io.github.milczekt1.corral.rules.spring.configurationpropertiesmustuseprojectprefix.fixtures;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** MUST IGNORE: under the project prefix. */
@ConfigurationProperties("corral.payments")
public class PaymentProperties {
    private String apiKey;
}
