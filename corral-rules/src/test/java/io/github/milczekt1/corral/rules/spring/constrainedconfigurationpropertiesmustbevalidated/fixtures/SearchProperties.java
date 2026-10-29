package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures;

import jakarta.validation.Valid;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** FLAGGED: {@code jakarta} {@code @Valid} alone counts as a constraint. */
@ConfigurationProperties("search")
public class SearchProperties {
    @Valid
    private SearchIndex index = new SearchIndex();
}
