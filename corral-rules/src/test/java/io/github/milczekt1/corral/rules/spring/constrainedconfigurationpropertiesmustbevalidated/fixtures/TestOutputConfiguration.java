package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures;

import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.production.ClientSettings;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MUST IGNORE where it compiled, in test output; MUST FLAG once copied into a production layout.
 */
@Configuration
public class TestOutputConfiguration {
    /** MUST IGNORE where it compiled, in test output; MUST FLAG once copied into a production layout. */
    @Bean
    @ConfigurationProperties("report")
    public ClientSettings reportSettings() {
        return new ClientSettings();
    }
}
