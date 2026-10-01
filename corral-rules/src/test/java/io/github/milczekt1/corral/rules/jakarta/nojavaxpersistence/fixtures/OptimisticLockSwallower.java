package io.github.milczekt1.corral.rules.jakarta.nojavaxpersistence.fixtures;

import javax.persistence.OptimisticLockException;

/** MUST FLAG: a catch clause alone is a dependency. */
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
