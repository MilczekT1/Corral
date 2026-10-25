package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** MUST IGNORE: the self-referencing {@link TreeNode} carries no constraint. */
@ConfigurationProperties("tree")
public class TreeProperties {
    private TreeNode root = new TreeNode();
}
