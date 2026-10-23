package io.github.milczekt1.corral.rules.naming.lowercasepackages.fixtures.paymentprocessing;

/** MUST IGNORE: a lowercase package, though the class name itself is mixed case. */
public class PaymentService {

    public boolean authorise(long amountInCents, long limitInCents) {
        return amountInCents <= limitInCents;
    }
}
