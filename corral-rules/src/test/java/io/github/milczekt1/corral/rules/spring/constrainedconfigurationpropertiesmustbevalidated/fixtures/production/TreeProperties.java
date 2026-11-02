package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.production;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** MUST IGNORE: has no constraints; it holds itself, so the search must terminate. */
@ConfigurationProperties("tree")
public class TreeProperties {
    private TreeProperties child;
}
