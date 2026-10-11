package io.github.milczekt1.corral.rules.java.nooptionalfields.fixtures;

import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** MUST IGNORE: a directly annotated {@code @Component}, and a record. */
@Component
public record TariffTable(Optional<BigDecimal> surcharge) {
}
