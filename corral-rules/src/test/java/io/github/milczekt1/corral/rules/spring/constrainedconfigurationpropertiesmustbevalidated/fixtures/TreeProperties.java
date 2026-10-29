package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** MUST IGNORE: holds itself, with no constraints, so the descent must terminate. */
@ConfigurationProperties("tree")
public class TreeProperties {
    private TreeProperties child;
}
