package io.github.milczekt1.corral.rules.spring.nofinalcomponent.fixtures;

import org.springframework.context.annotation.Configuration;

/** MUST FLAG: never enhanced for its {@code @Bean} methods, but an aspect matching it still proxies it. */
@Configuration(proxyBeanMethods = false)
public final class PaymentConfig {

    public String currency() {
        return "EUR";
    }
}
