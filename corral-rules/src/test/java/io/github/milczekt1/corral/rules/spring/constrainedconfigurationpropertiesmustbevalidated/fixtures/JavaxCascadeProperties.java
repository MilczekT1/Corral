package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures;

import javax.validation.Valid;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** FLAGGED: {@code javax} {@code @Valid} alone counts as a constraint. */
@ConfigurationProperties("legacy-search")
public class JavaxCascadeProperties {
    @Valid
    private SearchIndex index = new SearchIndex();
}
