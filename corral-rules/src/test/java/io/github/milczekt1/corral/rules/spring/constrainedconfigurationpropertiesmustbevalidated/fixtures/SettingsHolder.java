package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** MUST IGNORE: not bound; its inner {@link Section} references it only through {@code this$0}. */
public class SettingsHolder {
    @NotBlank
    private String owner = "ops";

    /** MUST IGNORE: bound and unconstrained; the synthetic outer reference is not a property. */
    @ConfigurationProperties("section")
    public class Section {
        private String title;

        public String describe() {
            return owner + ":" + title;
        }
    }
}
