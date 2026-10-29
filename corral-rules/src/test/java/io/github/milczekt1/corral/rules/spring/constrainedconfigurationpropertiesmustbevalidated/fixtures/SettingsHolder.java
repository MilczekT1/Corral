package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** NOT FLAGGED: not bound. */
public class SettingsHolder {
    @NotBlank
    private String owner = "ops";

    /** NOT FLAGGED: its only link to the constrained outer class is the synthetic {@code this$0}. */
    @ConfigurationProperties("section")
    public class Section {
        private String title;

        public String describe() {
            return owner + ":" + title;
        }
    }
}
