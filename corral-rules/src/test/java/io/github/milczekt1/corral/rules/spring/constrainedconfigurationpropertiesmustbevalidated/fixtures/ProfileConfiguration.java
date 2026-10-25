package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** MUST IGNORE {@code profile()}: only the declared return type is read, and it is unconstrained. */
@Configuration
public class ProfileConfiguration {
    @Bean
    @ConfigurationProperties("profile")
    public ProfileBase profile() {
        return new ConstrainedProfile();
    }
}
