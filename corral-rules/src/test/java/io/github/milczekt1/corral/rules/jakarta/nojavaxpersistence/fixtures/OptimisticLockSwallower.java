package io.github.milczekt1.corral.rules.jakarta.nojavaxpersistence.fixtures;

import javax.persistence.OptimisticLockException;

/** NOT SEEN: ArchUnit records no dependency for a catch clause, and the rule's docs say so. */
public class OptimisticLockSwallower {

    public boolean trySave(Runnable save) {
        try {
            save.run();
            return true;
        } catch (OptimisticLockException e) {
            return false;
        }
    }
}
