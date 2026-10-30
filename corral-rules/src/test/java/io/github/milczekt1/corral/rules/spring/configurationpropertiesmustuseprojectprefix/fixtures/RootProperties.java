package io.github.milczekt1.corral.rules.spring.configurationpropertiesmustuseprojectprefix.fixtures;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** MUST FLAG: no prefix at all, so it binds from the root of every property source. */
@ConfigurationProperties
public class RootProperties {
    private String region;
}
