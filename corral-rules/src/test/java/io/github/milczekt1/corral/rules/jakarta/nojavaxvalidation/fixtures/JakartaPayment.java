package io.github.milczekt1.corral.rules.jakarta.nojavaxvalidation.fixtures;

import jakarta.validation.constraints.NotBlank;

/** MUST IGNORE: the migrated namespace. */
public record JakartaPayment(@NotBlank String reference) {
}
