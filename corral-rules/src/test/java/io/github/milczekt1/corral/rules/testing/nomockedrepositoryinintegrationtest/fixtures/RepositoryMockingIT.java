package io.github.milczekt1.corral.rules.testing.nomockedrepositoryinintegrationtest.fixtures;

import org.mockito.Mock;

/** MUST FLAG: {@code @Mock} on a type ending in Repository, in a class named {@code *IT}. */
public class RepositoryMockingIT {

    @Mock
    OrderRepository mockedOrderRepository;
}
