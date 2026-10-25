package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** MUST IGNORE: a setter parameter constraint. */
@ConfigurationProperties("proxy")
public class ProxyProperties {
    private String host;

    public void setHost(@NotBlank String host) {
        this.host = host;
    }
}
