package io.github.milczekt1.llamarules.doc;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.experimental.UtilityClass;

@UtilityClass
public class RuleRegistry {

    private static final Map<String, RuleDoc> DOCS = new ConcurrentHashMap<>();

    public static void register(RuleDoc doc) {
        RuleDoc alreadyUnderThisId = DOCS.putIfAbsent(doc.id(), doc);

        if (alreadyUnderThisId == null) {
            return;
        }

        // The id alone decides nothing: the same rule registering again carries the same
        // documentation, so only a difference in why/howToFix/howNotToFix is a real collision.
        boolean sameDocumentationWordForWord = alreadyUnderThisId.equals(doc);
        if (sameDocumentationWordForWord) {
            return;
        }

        throw new IllegalStateException(
                "Duplicate rule id '" + doc.id() + "': two rules claim it with different"
                        + " documentation. Rule ids are freeze-store keys and must be globally"
                        + " unique — registering the identical doc again is fine, this is not."
                        + "\n  already registered: " + alreadyUnderThisId
                        + "\n  rejected:           " + doc);
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
