package io.github.milczekt1.corral.rules.java.nooptionalmethodparameters.fixtures;

import java.util.Optional;

/** MUST IGNORE: a record component reaches methods only as the accessor's return type and the constructor's parameter. */
public record RefundView(String id, Optional<String> reason) {
}
