package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures;

import javax.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** MUST FLAG: a {@code javax.validation} constraint. */
@ConfigurationProperties("legacy-queue")
public class JavaxNotNullProperties {
    @NotNull
    private String queue;
}
