package io.github.milczekt1.corral.rules.spring.notransactionalonfinalorstatic.fixtures;

import org.springframework.transaction.annotation.Transactional;

/**
 * MUST IGNORE: a transactional superclass with no methods of its own; its annotation reaches every
 * subclass's instance methods.
 */
@Transactional
public abstract class CarrierBase {

    protected int parcels;
}
