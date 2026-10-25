package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** MUST IGNORE {@code storageSettings()}: the class it binds is {@code @Validated}. */
@Configuration
public class StorageConfiguration {
    @Bean
    @ConfigurationProperties("storage")
    public StorageSettings storageSettings() {
        return new StorageSettings();
    }
}
