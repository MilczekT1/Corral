package io.github.milczekt1.corral.rules.jakarta.nojavaxvalidation.fixtures;

import javax.validation.constraints.NotBlank;

/** MUST FLAG the {@code javax} field only: the {@code jakarta} one beside it must not be reported. */
public record HalfMigratedOrder(@NotBlank String reference,
                                @jakarta.validation.constraints.NotNull Integer quantity) {
}
