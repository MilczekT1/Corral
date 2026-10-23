package io.github.milczekt1.corral.rules.spring.novalueonfield.fixtures;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;

/** MUST IGNORE: javac puts {@code @Value} on the record's final field as well as the constructor parameter. */
public record RefundSettings(@Value("${app.timeout}") Duration timeout) {
}
