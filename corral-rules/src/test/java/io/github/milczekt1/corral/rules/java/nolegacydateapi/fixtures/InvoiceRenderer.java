package io.github.milczekt1.corral.rules.java.nolegacydateapi.fixtures;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/** MUST FLAG the {@code Date} field and the {@code SimpleDateFormat}; the {@code java.time} types beside them are fine. */
public class InvoiceRenderer {

    private Date issuedAt;

    private LocalDate dueOn;

    public String render() {
        return new SimpleDateFormat("yyyy-MM-dd").format(issuedAt) + " / " + DateTimeFormatter.ISO_LOCAL_DATE.format(dueOn);
    }
}
