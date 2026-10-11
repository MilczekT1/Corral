package io.github.milczekt1.corral.rules.java.nooptionalfields.fixtures;

import java.util.Optional;
import org.springframework.context.annotation.Configuration;

/** MUST IGNORE: {@code @Configuration} reaches {@code @Component} only through a meta-annotation. */
@Configuration
public class PaymentConfig {

    private Optional<String> acquirer = Optional.empty();

    public String acquirerOrDefault() {
        return acquirer.orElse("default");
    }
}
