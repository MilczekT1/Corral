package io.github.milczekt1.corral.rules.spring.nocomponentonabstractclass.fixtures.production;

/** MUST IGNORE: abstract, but carries no stereotype. */
public abstract class AbstractPaymentHandler {

    public abstract boolean pay(long cents);
}
