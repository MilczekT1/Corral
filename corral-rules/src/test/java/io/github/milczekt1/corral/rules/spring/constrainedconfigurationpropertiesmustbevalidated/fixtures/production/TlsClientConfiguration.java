package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.production;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** MUST FLAG: an override of a binding method. */
@Configuration
public class TlsClientConfiguration extends ClientConfiguration {
    /** MUST FLAG once; the synthetic bridge javac adds, returning {@link ClientSettings}, MUST IGNORE. */
    @Override
    @Bean
    @ConfigurationProperties("client")
    public TlsClientSettings clientSettings() {
        return new TlsClientSettings();
    }
}
