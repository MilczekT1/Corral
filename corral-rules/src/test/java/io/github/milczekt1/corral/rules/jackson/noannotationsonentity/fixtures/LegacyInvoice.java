package io.github.milczekt1.corral.rules.jackson.noannotationsonentity.fixtures;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import javax.persistence.Entity;
import javax.persistence.Id;

/** MUST FLAG {@code issuedOn} once, though {@code @JsonFormat} has enum-typed members: a javax entity. */
@Entity
public class LegacyInvoice {

    @Id
    private Long id;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate issuedOn;
}
