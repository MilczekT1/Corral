package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** MUST FLAG {@code exportSettings()}: an unvalidated binding method. */
@Configuration
public class ExportConfiguration {
    @Bean
    @ConfigurationProperties("export")
    public ExportSettings exportSettings() {
        return new ExportSettings();
    }
}
