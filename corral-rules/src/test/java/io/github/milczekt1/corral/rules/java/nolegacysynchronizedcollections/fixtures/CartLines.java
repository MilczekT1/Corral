package io.github.milczekt1.corral.rules.java.nolegacysynchronizedcollections.fixtures;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

/** MUST FLAG the {@code Vector} and {@code Hashtable} fields; the {@code ArrayList} beside them is fine. */
public class CartLines {

    private final Vector<String> items = new Vector<>();

    private final Hashtable<String, String> labels = new Hashtable<>();

    private final List<String> tags = new ArrayList<>();

    public int size() {
        return items.size() + labels.size() + tags.size();
    }
}
