package io.github.milczekt1.corral.rules.testing.nomockedrepositoryinintegrationtest.fixtures;

import org.mockito.Mock;

/** MUST IGNORE: mocking a repository is what a unit test is for. */
public class RepositoryMockingUnitTest {

    @Mock
    OrderRepository unitTestOrderRepository;
}
