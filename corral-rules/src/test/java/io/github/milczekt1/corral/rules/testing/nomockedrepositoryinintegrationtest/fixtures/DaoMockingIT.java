package io.github.milczekt1.corral.rules.testing.nomockedrepositoryinintegrationtest.fixtures;

import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** MUST FLAG: the Dao half of the type clause, mocked through Spring's {@code @MockitoBean}. */
public class DaoMockingIT {

    @MockitoBean
    OrderDao mockedOrderDao;
}
