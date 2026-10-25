package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures;

import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** MUST FLAG: the constrained type is a {@code Map} value. */
@ConfigurationProperties("shards")
public class ShardMapProperties {
    private Map<String, Datasource> shards = Map.of();
}
