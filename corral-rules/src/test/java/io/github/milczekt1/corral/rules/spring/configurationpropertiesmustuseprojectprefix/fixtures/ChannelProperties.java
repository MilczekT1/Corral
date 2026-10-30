package io.github.milczekt1.corral.rules.spring.configurationpropertiesmustuseprojectprefix.fixtures;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** MUST FLAG: abstract, but it declares the prefix its subclasses bind under. */
@ConfigurationProperties("channel")
public abstract class ChannelProperties {
    private String name;
}
