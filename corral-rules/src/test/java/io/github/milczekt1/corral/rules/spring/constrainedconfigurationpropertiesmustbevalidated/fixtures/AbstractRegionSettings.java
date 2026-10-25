package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures;

import jakarta.validation.constraints.NotBlank;

/** MUST IGNORE: neither bound nor concrete; its subclass inherits the constrained field. */
public abstract class AbstractRegionSettings {
    @NotBlank
    private String region;
}
