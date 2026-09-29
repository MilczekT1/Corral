package io.github.milczekt1.corral.rules.dependencies.nojavax.fixtures;

import jakarta.validation.constraints.NotNull;

/** MUST IGNORE: the migrated namespace. */
public class JakartaOrderService {

    @NotNull
    private String customer;

    public String customer() {
        return customer;
    }
}
