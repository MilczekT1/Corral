package io.github.milczekt1.corral.rules.java.nofinalizeoverride.fixtures;

/** MUST FLAG {@code finalize()}: an override widened to public. */
@SuppressWarnings("deprecation")
public class PublicFinalizer {

    private int released;

    @Override
    public void finalize() {
        released++;
    }

    public int released() {
        return released;
    }
}
