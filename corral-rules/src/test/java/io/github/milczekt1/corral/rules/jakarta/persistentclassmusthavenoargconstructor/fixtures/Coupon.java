package io.github.milczekt1.corral.rules.jakarta.persistentclassmusthavenoargconstructor.fixtures;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/** MUST FLAG: a record has only its canonical constructor. */
@Entity
public record Coupon(@Id Long id, String code) {
}
