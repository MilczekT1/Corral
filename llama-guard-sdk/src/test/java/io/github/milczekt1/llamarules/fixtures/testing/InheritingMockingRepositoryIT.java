package io.github.milczekt1.llamarules.fixtures.testing;

/**
 * Declares no field of its own — the mocked repository arrives from
 * {@link AbstractMockingRepositoryTestBase}. Sharing a mock through a base test class is the usual
 * way this is written, so the rule has to see through the inheritance.
 */
public class InheritingMockingRepositoryIT extends AbstractMockingRepositoryTestBase {
}
