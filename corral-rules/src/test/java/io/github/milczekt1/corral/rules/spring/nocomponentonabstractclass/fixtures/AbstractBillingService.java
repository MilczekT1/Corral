package io.github.milczekt1.corral.rules.spring.nocomponentonabstractclass.fixtures;

import org.springframework.stereotype.Service;

/** MUST FLAG: an abstract {@code @Service}. */
@Service
public abstract class AbstractBillingService {

    public abstract long invoice(long cents);
}
