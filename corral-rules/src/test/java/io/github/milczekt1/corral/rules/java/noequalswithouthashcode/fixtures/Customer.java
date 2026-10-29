package io.github.milczekt1.corral.rules.java.noequalswithouthashcode.fixtures;

import java.util.Objects;

/** MUST IGNORE: {@code hashCode()} is inherited from a superclass other than {@code Object}. */
public final class Customer extends BaseEntity {

    @Override
    public boolean equals(Object o) {
        return o instanceof Customer other && Objects.equals(other.id, id);
    }
}
