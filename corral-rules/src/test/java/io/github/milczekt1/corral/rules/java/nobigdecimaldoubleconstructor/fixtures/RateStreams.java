package io.github.milczekt1.corral.rules.java.nobigdecimaldoubleconstructor.fixtures;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.DoubleStream;

/**
 * MUST FLAG {@code referenced}, a {@code BigDecimal::new} constructor reference, and {@code wrapped},
 * a lambda calling the constructor. {@code converted} uses {@code BigDecimal::valueOf} and must be
 * ignored.
 */
public class RateStreams {

    public List<BigDecimal> referenced(double[] rates) {
        return DoubleStream.of(rates).mapToObj(BigDecimal::new).toList();
    }

    public List<BigDecimal> wrapped(double[] rates) {
        return DoubleStream.of(rates).mapToObj(rate -> new BigDecimal(rate)).toList();
    }

    public List<BigDecimal> converted(double[] rates) {
        return DoubleStream.of(rates).mapToObj(BigDecimal::valueOf).toList();
    }
}
