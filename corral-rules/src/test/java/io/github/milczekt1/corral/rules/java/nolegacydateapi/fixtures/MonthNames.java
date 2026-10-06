package io.github.milczekt1.corral.rules.java.nolegacydateapi.fixtures;

import java.text.DateFormatSymbols;

/** MUST IGNORE: {@code DateFormatSymbols} shares a name prefix with {@code DateFormat} but is not a subtype. */
public class MonthNames {

    public String[] all() {
        return DateFormatSymbols.getInstance().getMonths();
    }
}
