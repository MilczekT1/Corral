package io.github.milczekt1.corral.rules.spring.norepositoryincontroller.fixtures;

import org.springframework.stereotype.Service;

/** Not an example: a service with no repository behind it. */
@Service
public class PricingService {

    public long quote(long orderId) {
        return orderId * 100;
    }
}
