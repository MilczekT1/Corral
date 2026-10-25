package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures;

import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;

/** MUST IGNORE: validated itself. */
@Validated
public class StorageSettings {
    @NotBlank
    private String bucket;
}
