package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** MUST IGNORE: validated, though the {@link Datasource} it holds is constrained. */
@Validated
@ConfigurationProperties("outer")
public class ValidatedOuterProperties {
    private Datasource primary = new Datasource();
}
