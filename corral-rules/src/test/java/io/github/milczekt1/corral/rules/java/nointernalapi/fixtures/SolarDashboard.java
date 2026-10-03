package io.github.milczekt1.corral.rules.java.nointernalapi.fixtures;

import io.github.milczekt1.corral.rules.java.nointernalapi.fixtures.sun.SolarReading;

/** MUST IGNORE: depends on a project package with a {@code sun} segment, not the root {@code sun} package. */
public class SolarDashboard {

    public double output(SolarReading reading) {
        return reading.watts();
    }
}
