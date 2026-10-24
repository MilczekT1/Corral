package io.github.milczekt1.corral.rules.jackson.noannotationsonentity.fixtures;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;

/** MUST FLAG {@code createdAt}: a Jakarta mapped superclass is the schema too. */
@MappedSuperclass
public abstract class AuditedBase {

    @JsonProperty("created_at")
    private Instant createdAt;
}
