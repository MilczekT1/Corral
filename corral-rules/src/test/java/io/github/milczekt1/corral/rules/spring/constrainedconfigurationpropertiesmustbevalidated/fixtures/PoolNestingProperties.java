package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures;

import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** MUST FLAG: the constrained type is nested two type arguments deep. */
@ConfigurationProperties("pools")
public class PoolNestingProperties {
    private Map<String, List<Datasource>> pools = Map.of();
}
