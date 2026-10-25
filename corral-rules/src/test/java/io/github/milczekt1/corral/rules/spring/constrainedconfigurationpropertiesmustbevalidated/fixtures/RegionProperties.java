package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** MUST FLAG: inherits a constrained field from an unannotated abstract base. */
@ConfigurationProperties("region")
public class RegionProperties extends AbstractRegionSettings {
    private int zones;
}
