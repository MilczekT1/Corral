package io.github.milczekt1.corral.rules.spring.configurationpropertiesmustuseprojectprefix.fixtures;

/** MUST IGNORE: inherits {@link AppProperties}' binding, which is reported once, on {@code AppProperties}. */
public class TimeoutDefaults extends AppProperties {
    private int retries;
}
