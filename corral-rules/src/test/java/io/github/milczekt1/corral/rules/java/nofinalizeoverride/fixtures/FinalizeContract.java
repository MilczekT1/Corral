package io.github.milczekt1.corral.rules.java.nofinalizeoverride.fixtures;

/** MUST FLAG {@code finalize()}: it forces every implementor to install a finalizer. */
public interface FinalizeContract {

    void finalize();
}
