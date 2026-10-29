package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/** MUST IGNORE every method: each binding is validated, or its declared return type is unconstrained. */
@Configuration
public class UnflaggedBindingsConfiguration {
    /** Boot validates this binding; {@link QueueProperties} is still flagged for its own annotation. */
    @Bean
    @Validated
    @ConfigurationProperties("queue")
    public QueueProperties queueProperties() {
        return new QueueProperties();
    }

    /** {@link OrderProperties} is {@code @Validated} itself, which covers this binding. */
    @Bean
    @ConfigurationProperties("orders")
    public OrderProperties orderProperties() {
        return new OrderProperties();
    }

    /** Only the declared return type is read, and {@link TenantBase} is unconstrained. */
    @Bean
    @ConfigurationProperties("tenant")
    public TenantBase tenant() {
        return new TenantProperties();
    }
}
