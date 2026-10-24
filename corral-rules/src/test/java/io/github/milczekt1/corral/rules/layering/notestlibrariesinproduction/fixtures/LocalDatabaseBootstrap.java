package io.github.milczekt1.corral.rules.layering.notestlibrariesinproduction.fixtures;

import java.util.List;
import org.testcontainers.containers.GenericContainer;

/** MUST FLAG {@code org.testcontainers..}: a container started from a production dev profile. */
public class LocalDatabaseBootstrap {

    GenericContainer<?> start() {
        GenericContainer<?> postgres = new GenericContainer<>("postgres:17");
        postgres.setExposedPorts(List.of(5432));
        postgres.start();
        return postgres;
    }
}
