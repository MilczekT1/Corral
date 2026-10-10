package io.github.milczekt1.corral.springboot3.rules.testing.nolegacymockbeanandspybean.fixtures;

import java.time.Clock;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/** MUST FLAG the {@code @SpyBean} field, and MUST IGNORE its replacement beside it. */
public class SpyBeanFieldHolder {

    @SpyBean
    Clock clock;

    @MockitoSpyBean
    Clock replacementClock;

    long now() {
        return clock.millis() + replacementClock.millis();
    }
}
