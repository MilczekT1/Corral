package io.github.milczekt1.corral.rules.testing.nomockedrepositoryinintegrationtest.fixtures;

/** SUPPORT: not a repository or dao, so mocking it in an integration test is fine. */
public interface PaymentGateway {

    boolean charge(long amountMinor);
}
