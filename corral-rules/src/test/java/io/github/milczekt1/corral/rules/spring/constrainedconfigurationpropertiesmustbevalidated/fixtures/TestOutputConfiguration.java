package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TestOutputConfiguration {
    /** NOT FLAGGED where it compiled, in test output; FLAGGED once copied into a production layout. */
    @Bean
    @ConfigurationProperties("report")
    public ClientSettings reportSettings() {
        return new ClientSettings();
    }
}
