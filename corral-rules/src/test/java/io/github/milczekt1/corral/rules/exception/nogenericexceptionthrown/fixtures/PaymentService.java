package io.github.milczekt1.corral.rules.exception.nogenericexceptionthrown.fixtures;

/** MUST IGNORE: throws a named subclass, and rethrows a caught throwable without constructing one. */
public class PaymentService {

    public void charge(long amount) {
        if (amount > 10_000) {
            throw new PaymentDeclinedException("limit exceeded");
        }
    }

    public void chargeAll(Iterable<Runnable> charges) {
        for (Runnable charge : charges) {
            try {
                charge.run();
            } catch (RuntimeException failure) {
                throw failure;
            }
        }
    }
}
