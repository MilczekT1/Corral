package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration
public class ClientConfiguration {
    /** FLAGGED: binds a constrained class, and neither is {@code @Validated}. */
    @Bean
    @ConfigurationProperties("client")
    public ClientSettings clientSettings() {
        return new ClientSettings();
    }

    /** NOT FLAGGED: the binding method is {@code @Validated}. */
    @Bean
    @Validated
    @ConfigurationProperties("validated-client")
    public ClientSettings validatedClientSettings() {
        return new ClientSettings();
    }

    /** NOT FLAGGED: a plain factory method, not a binding. */
    @Bean
    public ClientSettings fallbackClientSettings() {
        return new ClientSettings();
    }
}
