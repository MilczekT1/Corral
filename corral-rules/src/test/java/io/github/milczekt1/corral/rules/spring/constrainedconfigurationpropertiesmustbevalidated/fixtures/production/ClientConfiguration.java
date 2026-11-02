package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.production;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/** MUST FLAG: a {@code @Bean} method binds a constrained class. */
@Configuration
public class ClientConfiguration {
    /** MUST FLAG: binds a constrained class, and neither is {@code @Validated}. */
    @Bean
    @ConfigurationProperties("client")
    public ClientSettings clientSettings() {
        return new ClientSettings();
    }

    /** MUST IGNORE: the binding method is {@code @Validated}. */
    @Bean
    @Validated
    @ConfigurationProperties("validated-client")
    public ClientSettings validatedClientSettings() {
        return new ClientSettings();
    }

    /** MUST IGNORE: a plain factory method, not a binding. */
    @Bean
    public ClientSettings fallbackClientSettings() {
        return new ClientSettings();
    }
}
