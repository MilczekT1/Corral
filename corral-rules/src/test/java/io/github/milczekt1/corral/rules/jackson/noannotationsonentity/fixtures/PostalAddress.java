package io.github.milczekt1.corral.rules.jackson.noannotationsonentity.fixtures;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Embeddable;

/** MUST IGNORE: an embeddable is a value type, never proxied, and out of scope. */
@Embeddable
public class PostalAddress {

    @JsonProperty("post_code")
    private String postCode;
}
