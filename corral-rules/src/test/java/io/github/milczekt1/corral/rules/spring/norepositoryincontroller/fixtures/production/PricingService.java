package io.github.milczekt1.corral.rules.spring.norepositoryincontroller.fixtures.production;

import org.springframework.stereotype.Service;

/** MUST IGNORE: a service with no repository behind it. */
@Service
public class PricingService {

    public long quote(long orderId) {
        return orderId * 100;
    }
}
