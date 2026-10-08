package io.github.milczekt1.corral.rules.spring.nofinalcomponent.fixtures;

import org.springframework.context.annotation.Configuration;

/** MUST FLAG: a final {@code @Configuration}, which CGLIB enhances for its {@code @Bean} methods. */
@Configuration
public final class ShippingConfig {

    public String carrier() {
        return "post";
    }
}
