package io.github.milczekt1.corral.rules.jackson.noannotationsonentity.fixtures;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/** MUST IGNORE from test output; MUST FLAG {@code secret} once copied into a production layout. */
@Entity
public class TestOutputTicket {

    @Id
    private Long id;

    @JsonIgnore
    private String secret;
}
