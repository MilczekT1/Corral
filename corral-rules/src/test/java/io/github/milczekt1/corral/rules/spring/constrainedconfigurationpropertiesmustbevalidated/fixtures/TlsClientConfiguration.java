package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TlsClientConfiguration extends ClientConfiguration {
    /** FLAGGED once; the synthetic bridge javac adds, returning {@link ClientSettings}, NOT FLAGGED. */
    @Override
    @Bean
    @ConfigurationProperties("client")
    public TlsClientSettings clientSettings() {
        return new TlsClientSettings();
    }
}
