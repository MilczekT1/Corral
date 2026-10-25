package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/** MUST IGNORE {@code queueProperties()}: the binding method is {@code @Validated}. */
@Configuration
public class QueueConfiguration {
    @Bean
    @Validated
    @ConfigurationProperties("queue")
    public QueueProperties queueProperties() {
        return new QueueProperties();
    }
}
