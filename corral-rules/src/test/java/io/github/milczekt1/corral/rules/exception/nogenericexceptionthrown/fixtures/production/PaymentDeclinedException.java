package io.github.milczekt1.corral.rules.exception.nogenericexceptionthrown.fixtures.production;

import java.util.Optional;

/**
 * MUST FLAG a static factory returning {@code new RuntimeException}, and a
 * {@code RuntimeException::new} reference inside a constructor. MUST IGNORE {@code super(message)}.
 */
public class PaymentDeclinedException extends RuntimeException {

    public PaymentDeclinedException(String message) {
        super(message);
    }

    public PaymentDeclinedException(Optional<String> reason) {
        super(reason.orElseThrow(RuntimeException::new));
    }

    public static RuntimeException wrapping(String message) {
        return new RuntimeException(message);
    }
}
