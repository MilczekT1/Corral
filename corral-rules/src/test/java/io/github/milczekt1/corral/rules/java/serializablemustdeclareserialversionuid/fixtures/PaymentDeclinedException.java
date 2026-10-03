package io.github.milczekt1.corral.rules.java.serializablemustdeclareserialversionuid.fixtures;

/** MUST IGNORE: serializable through {@code Throwable}, never declared directly. */
public class PaymentDeclinedException extends RuntimeException {

    public PaymentDeclinedException(String message) {
        super(message);
    }
}
