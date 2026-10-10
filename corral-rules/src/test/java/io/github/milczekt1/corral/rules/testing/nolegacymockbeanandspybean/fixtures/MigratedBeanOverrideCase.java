package io.github.milczekt1.corral.rules.testing.nolegacymockbeanandspybean.fixtures;

import java.time.Clock;
import org.mockito.Mock;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/** MUST IGNORE: the bean-override replacements and a plain Mockito mock. */
public class MigratedBeanOverrideCase {

    @MockitoBean
    Clock mockedClock;

    @MockitoSpyBean
    Clock spiedClock;

    @Mock
    Clock plainClock;

    long now() {
        return mockedClock.millis() + spiedClock.millis() + plainClock.millis();
    }
}
