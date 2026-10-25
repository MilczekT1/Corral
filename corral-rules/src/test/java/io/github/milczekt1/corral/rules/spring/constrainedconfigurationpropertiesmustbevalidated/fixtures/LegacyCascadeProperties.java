package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures;

import javax.validation.Valid;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** MUST FLAG: {@code javax} {@code @Valid} alone. */
@ConfigurationProperties("legacy-cascade")
public class LegacyCascadeProperties {
    @Valid
    private SearchIndex index = new SearchIndex();
}
