package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** NOT FLAGGED: has no constraints; it holds itself, so the search must terminate. */
@ConfigurationProperties("tree")
public class TreeProperties {
    private TreeProperties child;
}
