package io.github.milczekt1.corral.rules.spring.configurationpropertiesmustuseprojectprefix.fixtures;

import java.util.Properties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** MUST IGNORE as a class: not annotated; its methods carry the verdicts. */
@Configuration
public class ClientConfiguration extends ConfigurationTemplate {
    /** MUST FLAG once: binds the project's own type outside the project prefix; javac copies the annotation onto the bridge. */
    @Bean
    @ConfigurationProperties("client")
    @Override
    public ClientSettings clientSettings() {
        return new ClientSettings();
    }

    /** MUST IGNORE: a library type bound to that library's own namespace. */
    @Bean
    @ConfigurationProperties("spring.datasource.hikari")
    public Properties hikariProperties() {
        return new Properties();
    }

    /** MUST IGNORE: binds the project's own type under the project prefix. */
    @Bean
    @ConfigurationProperties("corral.regional-client")
    public ClientSettings regionalClientSettings() {
        return new ClientSettings();
    }
}
