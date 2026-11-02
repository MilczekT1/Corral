package io.github.milczekt1.corral.rules.testing.nomockedrepositoryinintegrationtest.fixtures;

import org.mockito.Mock;

/** MUST IGNORE: mocked, but a gateway is not a persistence type. */
public class GatewayMockingIT {

    @Mock
    PaymentGateway mockedPaymentGateway;
}
