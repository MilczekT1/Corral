package io.github.milczekt1.corral.rules.spring.nofinalcomponent.fixtures;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** MUST FLAG: a record is implicitly final. */
@Component
public record PricingPolicy(BigDecimal margin) {
}
