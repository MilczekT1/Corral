package io.github.milczekt1.corral.fixtures.testing;

import org.mockito.Mock;

/**
 * Holds the mocked repository that {@link InheritingMockingRepositoryIT} inherits.
 *
 * <p>Deliberately not named {@code *IT}: the rule matches on the simple name, so this class is
 * never inspected on its own. Only the subclass is — which is the whole point of the fixture. If
 * the rule ever goes back to {@code getFields()}, this shape stops being reported.
 */
public abstract class AbstractMockingRepositoryTestBase {

    @Mock
    OrderRepository inheritedOrderRepository;
}
