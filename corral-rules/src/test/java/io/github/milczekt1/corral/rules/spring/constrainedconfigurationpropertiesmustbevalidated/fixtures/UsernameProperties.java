package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures;

import org.hibernate.validator.constraints.Length;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** MUST FLAG: a repeated Hibernate Validator {@code @Length} compiles to {@code Length$List}. */
@ConfigurationProperties("accounts")
public class UsernameProperties {
    @Length(min = 3)
    @Length(max = 20)
    private String username;
}
