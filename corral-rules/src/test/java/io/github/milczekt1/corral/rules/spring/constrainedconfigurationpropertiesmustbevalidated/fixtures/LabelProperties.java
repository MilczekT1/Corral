package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** MUST IGNORE: its only annotation is meta-annotated with a constraint but is not one. */
@ConfigurationProperties("trimmed")
public class LabelProperties {
    @Trimmed
    private String label;
}
