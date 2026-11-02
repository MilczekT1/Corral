package io.github.milczekt1.corral.rules.spring.configurationpropertiesmustuseprojectprefix.fixtures.production;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** MUST IGNORE: the project prefix itself. */
@ConfigurationProperties("corral")
public class CorralProperties {
    private String name;
}
