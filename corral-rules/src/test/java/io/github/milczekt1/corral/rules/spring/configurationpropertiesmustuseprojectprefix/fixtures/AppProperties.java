package io.github.milczekt1.corral.rules.spring.configurationpropertiesmustuseprojectprefix.fixtures;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** MUST FLAG: a generic prefix outside the project prefix. */
@ConfigurationProperties("app")
public class AppProperties {
    private Duration timeout;
}
