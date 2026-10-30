package io.github.milczekt1.corral.rules.spring.configurationpropertiesmustuseprojectprefix.fixtures;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** MUST IGNORE where it compiled, in test output: only production classes are checked. */
@ConfigurationProperties("test")
public class TestOutputProperties {
    private String fixture;
}
