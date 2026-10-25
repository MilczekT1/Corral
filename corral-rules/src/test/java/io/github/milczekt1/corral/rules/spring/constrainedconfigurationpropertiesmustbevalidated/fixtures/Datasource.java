package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures;

import jakarta.validation.constraints.NotBlank;

/** MUST IGNORE: constrained but not bound; the classes holding it are. */
public class Datasource {
    @NotBlank
    private String url;
}
