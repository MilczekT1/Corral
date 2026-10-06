package io.github.milczekt1.corral.rules.java.nolegacydateapi.fixtures;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** MUST IGNORE: {@code java.time} throughout, including a field named {@code date}. */
public class DeliverySchedule {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE;

    private LocalDate date;

    private Instant dispatchedAt;

    public String render() {
        return ISO.format(date) + " " + dispatchedAt;
    }
}
