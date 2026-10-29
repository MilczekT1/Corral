package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** MUST FLAG {@code clientSettings()} once; its synthetic bridge returning {@link ClientSettings} MUST NOT. */
@Configuration
public class TlsClientConfiguration extends ClientConfiguration {
    @Override
    @Bean
    @ConfigurationProperties("client")
    public TlsClientSettings clientSettings() {
        return new TlsClientSettings();
    }
}
