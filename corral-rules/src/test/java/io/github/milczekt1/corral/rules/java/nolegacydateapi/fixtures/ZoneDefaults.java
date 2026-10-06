package io.github.milczekt1.corral.rules.java.nolegacydateapi.fixtures;

import java.time.ZoneId;
import java.util.TimeZone;

/** MUST FLAG the {@code TimeZone} lookup; the {@code ZoneId} one beside it is fine. */
public class ZoneDefaults {

    public String utcIds() {
        return TimeZone.getTimeZone("UTC").getID() + ZoneId.of("UTC").getId();
    }
}
