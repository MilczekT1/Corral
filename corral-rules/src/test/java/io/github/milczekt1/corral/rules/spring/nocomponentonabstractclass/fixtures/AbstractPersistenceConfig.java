package io.github.milczekt1.corral.rules.spring.nocomponentonabstractclass.fixtures;

import org.springframework.context.annotation.Configuration;

/** MUST IGNORE: an abstract {@code @Configuration}, processed when reached through {@code @Import}. */
@Configuration
public abstract class AbstractPersistenceConfig {

    protected abstract String schema();
}
