package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** MUST FLAG: javac copies the record component's constraint onto the field. */
@ConfigurationProperties("mail")
public record MailProperties(@NotBlank String host) {
}
