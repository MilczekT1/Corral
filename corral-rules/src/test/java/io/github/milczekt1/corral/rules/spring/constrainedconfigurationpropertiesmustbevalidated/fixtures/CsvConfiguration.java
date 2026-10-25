package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** MUST FLAG {@code exportSettings()} once; its synthetic bridge returning {@link ExportSettings} MUST NOT. */
@Configuration
public class CsvConfiguration extends ExportConfiguration {
    @Override
    @Bean
    @ConfigurationProperties("export")
    public CsvSettings exportSettings() {
        return new CsvSettings();
    }
}
