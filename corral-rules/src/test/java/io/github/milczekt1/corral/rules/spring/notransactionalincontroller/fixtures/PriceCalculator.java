package io.github.milczekt1.corral.rules.spring.notransactionalincontroller.fixtures;

import org.springframework.transaction.annotation.Transactional;

/** MUST IGNORE: {@code @Transactional} on a class with no stereotype. */
public class PriceCalculator {

    @Transactional
    public long price(long base) {
        return base * 2;
    }
}
