package io.github.milczekt1.archrules.fixtures.testing;

/** Not a repository or dao — mocking this in an IT is fine. */
public interface PaymentGateway {
    boolean charge(long amountMinor);
}
