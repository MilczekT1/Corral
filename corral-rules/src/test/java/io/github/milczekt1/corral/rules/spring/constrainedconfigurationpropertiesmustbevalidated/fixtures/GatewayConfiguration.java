package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/** MUST IGNORE {@code gatewaySettings()}: the binding method is {@code @Validated}. */
@Configuration
public class GatewayConfiguration {
    @Bean
    @Validated
    @ConfigurationProperties("gateway")
    public GatewaySettings gatewaySettings() {
        return new GatewaySettings();
    }
}
