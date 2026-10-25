package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** MUST IGNORE {@code reportSettings()} where it compiled, in test output; MUST FLAG once copied. */
@Configuration
public class TestOutputConfiguration {
    @Bean
    @ConfigurationProperties("report")
    public ReportSettings reportSettings() {
        return new ReportSettings();
    }
}
