package io.github.milczekt1.corral.rules.jackson.noannotationsonentity.fixtures;

import com.fasterxml.jackson.annotation.JsonProperty;

/** MUST IGNORE: the wire contract as its own type. */
public record PayoutResponse(@JsonProperty("payout_id") Long id) {
}
