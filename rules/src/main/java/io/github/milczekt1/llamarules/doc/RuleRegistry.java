package io.github.milczekt1.llamarules.doc;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.experimental.UtilityClass;

/** Global lookup from a rule's stable id to its {@link RuleDoc}. */
@UtilityClass
public class RuleRegistry {

    private static final Map<String, RuleDoc> DOCS = new ConcurrentHashMap<>();

    /**
     * @throws IllegalStateException if a <em>different</em> doc is already registered under this id
     */
    public static void register(RuleDoc doc) {
        RuleDoc alreadyUnderThisId = DOCS.putIfAbsent(doc.id(), doc);

        boolean idWasFree = alreadyUnderThisId == null;
        boolean sameDocAgain = !idWasFree && alreadyUnderThisId.equals(doc);
        if (idWasFree || sameDocAgain) {
            return;
        }

        throw new IllegalStateException(
                "Duplicate rule id '" + doc.id() + "': it is already registered with different"
                        + " documentation. Rule ids are freeze-store keys and must be globally unique.");
    }

    public static Optional<RuleDoc> find(String ruleDescription) {
        return ruleDescription == null
                ? Optional.empty()
                : Optional.ofNullable(DOCS.get(ruleDescription));
    }

    public static List<RuleDoc> all() {
        return DOCS.values().stream().sorted(Comparator.comparing(RuleDoc::id)).toList();
    }
}
