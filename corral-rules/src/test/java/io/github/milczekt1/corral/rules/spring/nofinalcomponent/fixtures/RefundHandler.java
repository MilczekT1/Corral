package io.github.milczekt1.corral.rules.spring.nofinalcomponent.fixtures;

/** MUST FLAG: final, and a bean only through {@link UseCase}. */
@UseCase
public final class RefundHandler {

    public boolean refund() {
        return true;
    }
}
