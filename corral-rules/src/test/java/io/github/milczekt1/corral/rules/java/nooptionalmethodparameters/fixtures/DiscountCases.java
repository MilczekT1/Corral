package io.github.milczekt1.corral.rules.java.nooptionalmethodparameters.fixtures;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/** MUST IGNORE wherever it compiles: JUnit supplies the argument, and the test method makes this a test class. */
public class DiscountCases {

    static Stream<Optional<String>> codes() {
        return Stream.of(Optional.empty(), Optional.of("SPRING"));
    }

    @ParameterizedTest
    @MethodSource("codes")
    void appliesDiscount(Optional<String> code) {
        assertNotNull(code);
    }
}
