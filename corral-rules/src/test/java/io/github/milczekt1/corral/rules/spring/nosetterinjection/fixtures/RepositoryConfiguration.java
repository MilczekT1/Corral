package io.github.milczekt1.corral.rules.spring.nosetterinjection.fixtures;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** MUST IGNORE: a {@code @Bean} factory method is not injection into the configuration class. */
@Configuration
public class RepositoryConfiguration {

    @Bean
    public OrderRepository orderRepository() {
        return new OrderRepository();
    }
}
