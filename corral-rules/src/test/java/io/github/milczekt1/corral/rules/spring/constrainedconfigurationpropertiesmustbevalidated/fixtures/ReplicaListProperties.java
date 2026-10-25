package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** MUST FLAG: the constrained type is a {@code List} element. */
@ConfigurationProperties("replicas")
public class ReplicaListProperties {
    private List<Datasource> replicas = List.of();
}
