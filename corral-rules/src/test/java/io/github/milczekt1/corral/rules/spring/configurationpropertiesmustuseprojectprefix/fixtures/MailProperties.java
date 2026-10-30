package io.github.milczekt1.corral.rules.spring.configurationpropertiesmustuseprojectprefix.fixtures;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** MUST FLAG: starts with the project prefix as text, but is another namespace. MUST IGNORE the {@code @Value} key. */
@ConfigurationProperties("corralx.mail")
public class MailProperties {
    private String host;

    @Value("${server.port}")
    private int port;
}
