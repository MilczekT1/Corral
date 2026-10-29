package io.github.milczekt1.corral.rules.java.noequalswithouthashcode.fixtures;

import java.util.Objects;

/** MUST IGNORE: {@code hashCode()} alone keeps the contract. */
public abstract class BaseEntity {

    protected Long id;

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
