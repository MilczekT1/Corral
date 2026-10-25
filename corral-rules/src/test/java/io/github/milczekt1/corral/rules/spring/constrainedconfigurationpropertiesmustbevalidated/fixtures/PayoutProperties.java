package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** MUST FLAG: the only constraint is a project annotation meta-annotated {@code @Constraint}. */
@ConfigurationProperties("payout")
public class PayoutProperties {
    @Iban
    private String account;
}
