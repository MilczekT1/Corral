package io.github.milczekt1.corral.rules.spring.nocomponentonabstractclass.fixtures;

/** MUST FLAG: abstract, and a stereotype only through {@link DomainService}. */
@DomainService
public abstract class AbstractLedgerPolicy {

    public abstract boolean allows(long cents);
}
