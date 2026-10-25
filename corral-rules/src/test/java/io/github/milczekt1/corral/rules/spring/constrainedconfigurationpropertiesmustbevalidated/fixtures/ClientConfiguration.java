package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** MUST FLAG {@code clientSettings()}: neither the method nor {@link ClientSettings} is {@code @Validated}. */
@Configuration
public class ClientConfiguration {
    @Bean
    @ConfigurationProperties("client")
    public ClientSettings clientSettings() {
        return new ClientSettings();
    }

    /** MUST IGNORE: a plain factory method, not a binding. */
    @Bean
    public ClientSettings fallbackClientSettings() {
        return new ClientSettings();
    }
}
