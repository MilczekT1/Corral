package io.github.milczekt1.llamarules.rules.fixtures.testing;

import org.springframework.test.context.bean.override.mockito.MockitoBean;

public class MockingDaoIntegrationTest {
    @MockitoBean
    OrderDao orderDao;
}
