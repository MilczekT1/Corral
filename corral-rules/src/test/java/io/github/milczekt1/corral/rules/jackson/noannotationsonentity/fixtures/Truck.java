package io.github.milczekt1.corral.rules.jackson.noannotationsonentity.fixtures;

import jakarta.persistence.Entity;

/** MUST IGNORE: annotations its superclass carries are reported there, not here. */
@Entity
public class Truck extends Vehicle {

    private int axles;
}
