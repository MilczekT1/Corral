package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures;

import jakarta.validation.constraints.NotBlank;

/** MUST IGNORE as a class: bound only through binding methods. */
public class ExportSettings {
    @NotBlank
    private String format;
}
