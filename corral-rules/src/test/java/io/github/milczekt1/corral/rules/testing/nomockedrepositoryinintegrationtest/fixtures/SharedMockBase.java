package io.github.milczekt1.corral.rules.testing.nomockedrepositoryinintegrationtest.fixtures;

import org.mockito.Mock;

/**
 * MUST FLAG its mock, the reported field, once for each {@code *IT} that inherits it. Not named
 * {@code *IT} itself, so it is not an integration test: only {@link InheritedMockingIT} is.
 */
public abstract class SharedMockBase {

    @Mock
    OrderRepository inheritedOrderRepository;
}
