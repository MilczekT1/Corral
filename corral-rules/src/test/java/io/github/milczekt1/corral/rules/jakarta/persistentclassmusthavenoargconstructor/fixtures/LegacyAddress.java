package io.github.milczekt1.corral.rules.jakarta.persistentclassmusthavenoargconstructor.fixtures;

import javax.persistence.Embeddable;

/** MUST FLAG: no {@code javax} provider builds a record embeddable. */
@Embeddable
public record LegacyAddress(String street, String city) {
}
