package io.github.milczekt1.corral.rules.jakarta.persistentclassmusthavenoargconstructor.fixtures;

import jakarta.persistence.Embeddable;

/** MUST IGNORE: Jakarta Persistence 3.2 builds a record embeddable through its canonical constructor. */
@Embeddable
public record GeoPoint(double latitude, double longitude) {
}
