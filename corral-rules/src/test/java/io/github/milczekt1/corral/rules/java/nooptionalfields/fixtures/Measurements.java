package io.github.milczekt1.corral.rules.java.nooptionalfields.fixtures;

import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

/** MUST FLAG each primitive variant, one field apiece. */
public class Measurements {

    private OptionalInt retries = OptionalInt.empty();

    private OptionalLong elapsed = OptionalLong.empty();

    private OptionalDouble ratio = OptionalDouble.empty();

    public String summary() {
        return retries + " " + elapsed + " " + ratio;
    }
}
