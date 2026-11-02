package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.production;

/** FLAGGED: inherits both {@code @ConfigurationProperties} and the constrained field. */
public class SmsChannelProperties extends ChannelProperties {
    private String gateway;
}
