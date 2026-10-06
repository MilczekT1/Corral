package io.github.milczekt1.corral.rules.java.nolegacydateapi.fixtures;

import java.text.DateFormat;

/** MUST FLAG: takes a {@code DateFormat} parameter. */
public class LedgerExporter {

    private final StringBuilder out = new StringBuilder();

    public void export(DateFormat format, long epochMillis) {
        out.append(format.format(epochMillis));
    }
}
