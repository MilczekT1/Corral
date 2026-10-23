package io.github.milczekt1.corral.rules.spring.noapplicationcontextinjection.fixtures;

import org.springframework.boot.SpringApplication;

/** MUST IGNORE: {@code run} returns a {@code ConfigurableApplicationContext}, but a discarded result is no dependency. */
public class ShopApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShopApplication.class, args);
    }
}
