package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** FLAGGED: a repeated constraint compiles to its {@code @Repeatable} container. */
@ConfigurationProperties("catalog")
public class CatalogProperties {
    @Sku
    @Sku(message = "too long")
    private String featured;
}
