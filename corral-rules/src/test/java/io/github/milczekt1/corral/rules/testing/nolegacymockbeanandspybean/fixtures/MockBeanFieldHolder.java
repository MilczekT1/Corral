package io.github.milczekt1.corral.rules.testing.nolegacymockbeanandspybean.fixtures;

import java.time.Clock;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** MUST FLAG the {@code @MockBean} field, and MUST IGNORE its replacement beside it. */
public class MockBeanFieldHolder {

    @MockBean
    Clock clock;

    @MockitoBean
    Clock replacementClock;

    long now() {
        return clock.millis() + replacementClock.millis();
    }
}
