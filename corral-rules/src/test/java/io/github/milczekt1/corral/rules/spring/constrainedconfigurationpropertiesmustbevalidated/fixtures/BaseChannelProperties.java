package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** MUST IGNORE: abstract, so Boot never binds it; its concrete subclass is reported instead. */
@ConfigurationProperties("channel")
public abstract class BaseChannelProperties {
    @NotBlank
    private String sender;
}
