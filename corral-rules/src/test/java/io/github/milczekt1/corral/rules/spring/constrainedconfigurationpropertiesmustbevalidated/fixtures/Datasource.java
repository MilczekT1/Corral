package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures;

import jakarta.validation.constraints.NotBlank;

/** NOT FLAGGED: constrained, but not bound; the class holding it is. */
public class Datasource {
    @NotBlank
    private String url;
}
