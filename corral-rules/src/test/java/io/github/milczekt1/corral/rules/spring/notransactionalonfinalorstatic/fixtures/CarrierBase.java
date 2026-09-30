package io.github.milczekt1.corral.rules.spring.notransactionalonfinalorstatic.fixtures;

import org.springframework.transaction.annotation.Transactional;

/** A transactional superclass: its annotation reaches every subclass's instance methods. */
@Transactional
public abstract class CarrierBase {

    protected int parcels;
}
