package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures;

import jakarta.validation.constraints.NotBlank;

/** MUST IGNORE: constrained, but bound only behind its supertype {@link ProfileBase}. */
public class ConstrainedProfile extends ProfileBase {
    @NotBlank
    private String key;
}
