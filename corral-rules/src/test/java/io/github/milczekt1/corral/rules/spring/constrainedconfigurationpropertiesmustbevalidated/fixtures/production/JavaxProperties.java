package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.production;

import javax.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** MUST FLAG: a {@code javax} constraint on a field. */
@ConfigurationProperties("legacy")
public class JavaxProperties {
    @NotNull
    private String queue;
}
