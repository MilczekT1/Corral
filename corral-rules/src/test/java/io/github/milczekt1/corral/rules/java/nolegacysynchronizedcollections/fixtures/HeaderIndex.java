package io.github.milczekt1.corral.rules.java.nolegacysynchronizedcollections.fixtures;

import java.util.Hashtable;

/** MUST FLAG: returns a {@code Hashtable}. */
public class HeaderIndex {

    public Hashtable<String, String> headers() {
        Hashtable<String, String> headers = new Hashtable<>();
        headers.put("Accept", "application/json");
        return headers;
    }
}
