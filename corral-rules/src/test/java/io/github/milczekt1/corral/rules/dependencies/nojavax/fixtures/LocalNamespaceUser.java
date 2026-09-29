package io.github.milczekt1.corral.rules.dependencies.nojavax.fixtures;

import io.github.milczekt1.corral.rules.dependencies.nojavax.fixtures.javax.LocalNamespaceType;

/** MUST IGNORE: a package merely named {@code javax} below the root is not the {@code javax} namespace. */
public class LocalNamespaceUser {

    private final LocalNamespaceType type = new LocalNamespaceType();

    public String label() {
        return type.label();
    }
}
