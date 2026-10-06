package io.github.milczekt1.corral.rules.java.nolegacysynchronizedcollections.fixtures;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** MUST IGNORE: the modern replacements, including a {@code Collections.synchronizedList}. */
public class ModernCart {

    private final List<String> items = new ArrayList<>();

    private final Map<String, String> labels = new HashMap<>();

    private final Map<String, Integer> stock = new ConcurrentHashMap<>();

    private final List<String> shared = Collections.synchronizedList(new ArrayList<>());

    private final Deque<String> history = new ArrayDeque<>();

    public int size() {
        history.push("size");
        return items.size() + labels.size() + stock.size() + shared.size();
    }
}
