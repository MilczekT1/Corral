package io.github.milczekt1.corral.rules.logging.noprintstacktrace.fixtures;

import java.io.PrintWriter;
import java.io.StringWriter;

/** MUST FLAG the {@code PrintWriter} overload, rendering the trace into a string by hand. */
public class ReportWriter {

    public String render(Exception failure) {
        StringWriter buffer = new StringWriter();
        failure.printStackTrace(new PrintWriter(buffer));
        return buffer.toString();
    }
}
