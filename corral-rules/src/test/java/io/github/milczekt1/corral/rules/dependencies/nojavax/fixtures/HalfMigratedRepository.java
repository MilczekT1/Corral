package io.github.milczekt1.corral.rules.dependencies.nojavax.fixtures;

import jakarta.persistence.EntityManager;
import javax.validation.constraints.NotNull;

/** MUST FLAG the {@code javax} constraint only: the {@code jakarta} entity manager beside it must not be reported. */
public class HalfMigratedRepository {

    private final EntityManager entityManager;

    @NotNull
    private String tenant;

    public HalfMigratedRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public boolean isOpen() {
        return entityManager.isOpen();
    }

    public String tenant() {
        return tenant;
    }
}
