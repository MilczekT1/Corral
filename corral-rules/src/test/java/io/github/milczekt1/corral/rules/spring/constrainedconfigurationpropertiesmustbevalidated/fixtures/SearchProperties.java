package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures;

import jakarta.validation.Valid;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** MUST FLAG: {@code @Valid} alone is ignored without {@code @Validated}. */
@ConfigurationProperties("search")
public class SearchProperties {
    @Valid
    private SearchIndex index = new SearchIndex();
}
