package io.github.milczekt1.corral.fixtures.testing;

import org.mockito.Mock;

/**
 * Holds the mocked repository that {@link InheritingMockingRepositoryIT} inherits.
 *
 * <p>Deliberately not named {@code *IT}: the rule matches on the simple name, so only the subclass
 * is inspected.
 */
public abstract class AbstractMockingRepositoryTestBase {

    @Mock
    OrderRepository inheritedOrderRepository;
}
