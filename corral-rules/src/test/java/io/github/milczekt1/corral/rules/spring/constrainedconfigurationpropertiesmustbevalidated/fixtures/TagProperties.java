package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** MUST IGNORE: a constraint only on a type argument, which ArchUnit does not import. */
@ConfigurationProperties("tags")
public class TagProperties {
    private List<@NotBlank String> tags = List.of();
}
