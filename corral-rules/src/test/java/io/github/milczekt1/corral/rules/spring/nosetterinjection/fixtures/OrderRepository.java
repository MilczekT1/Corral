package io.github.milczekt1.corral.rules.spring.nosetterinjection.fixtures;

/** The collaborator every example injects; not itself under test. */
public class OrderRepository {

    public int count() {
        return 0;
    }
}
