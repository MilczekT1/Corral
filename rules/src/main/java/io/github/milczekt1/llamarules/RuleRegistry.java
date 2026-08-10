package io.github.milczekt1.llamarules;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.experimental.UtilityClass;

/**
 * Global lookup from a rule's stable id to its {@link RuleDoc}.
 *
 * <p>Populated as a side effect of {@link Freezer#freeze}, i.e. when a group class is
 * initialised. The failure formatter reads from here to re-attach rich prose to a violation.
 */
@UtilityClass
public class RuleRegistry {

    private static final Map<String, RuleDoc> DOCS = new ConcurrentHashMap<>();

    /**
     * @throws IllegalStateException if a <em>different</em> doc is already registered under this id
     */
    public static void register(RuleDoc doc) {
        RuleDoc existing = DOCS.putIfAbsent(doc.id(), doc);
        if (existing != null && !existing.equals(doc)) {
            throw new IllegalStateException(
                    "Duplicate rule id '" + doc.id() + "': it is already registered with different"
                            + " documentation. Rule ids are freeze-store keys and must be globally unique.");
        }
    }

    /** Never throws; an unknown or null description simply yields {@link Optional#empty()}. */
    public static Optional<RuleDoc> find(String id) {
        return id == null ? Optional.empty() : Optional.ofNullable(DOCS.get(id));
    }

    /** Every doc registered <em>so far</em>, sorted by id. See the design note on group loading. */
    public static List<RuleDoc> all() {
        return DOCS.values().stream().sorted(Comparator.comparing(RuleDoc::id)).toList();
    }
}
