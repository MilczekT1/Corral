package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** MUST IGNORE: holds only JDK types, which carry no constraints. */
@ConfigurationProperties("jdk")
public class JdkTypesProperties {
    private Duration timeout = Duration.ofSeconds(1);
    private List<String> hosts = List.of();
    private Map<String, String> labels = Map.of();
}
