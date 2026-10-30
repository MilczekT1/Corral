package io.github.milczekt1.corral.rules.spring.configurationpropertiesmustuseprojectprefix.fixtures;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** MUST FLAG: the {@code prefix} attribute, inside a namespace Spring Boot uses. */
@ConfigurationProperties(prefix = "server.audit")
public class AuditProperties {
    private boolean enabled;
}
