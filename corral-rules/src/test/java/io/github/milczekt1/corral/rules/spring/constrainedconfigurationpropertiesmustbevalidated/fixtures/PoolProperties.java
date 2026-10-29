package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures;

import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** FLAGGED: the only constraint is in {@link Datasource}, held two type arguments deep. */
@ConfigurationProperties("pools")
public class PoolProperties {
    private Map<String, List<Datasource>> pools = Map.of();
}
