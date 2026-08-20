package io.github.milczekt1.llamaguard.fixtures.testing;

import org.springframework.test.context.bean.override.mockito.MockitoBean;

public class MockingDaoIT {
    @MockitoBean
    OrderDao orderDao;
}
