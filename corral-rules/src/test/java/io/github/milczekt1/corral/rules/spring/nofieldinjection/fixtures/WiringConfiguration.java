package io.github.milczekt1.corral.rules.spring.nofieldinjection.fixtures;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** MUST IGNORE: {@code @Autowired} on a {@code @Bean} method parameter is not a field. */
@Configuration
public class WiringConfiguration {

    @Bean
    public MailSender mailSender(@Autowired OrderRepository outbox) {
        MailSender sender = new MailSender();
        sender.outbox = outbox;
        return sender;
    }
}
