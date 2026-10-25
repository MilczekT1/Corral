package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures;

import jakarta.validation.constraints.NotBlank;

/** MUST IGNORE: bound only by a method in test output. */
public class ReportSettings {
    @NotBlank
    private String recipient;
}
