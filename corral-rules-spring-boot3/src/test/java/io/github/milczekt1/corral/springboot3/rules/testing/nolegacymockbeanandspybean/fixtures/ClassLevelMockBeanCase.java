package io.github.milczekt1.corral.springboot3.rules.testing.nolegacymockbeanandspybean.fixtures;

import java.time.Clock;
import org.springframework.boot.test.mock.mockito.MockBean;

/** MUST FLAG: {@code @MockBean} on the class registers a mock with no field to hold it. */
@MockBean(Clock.class)
public class ClassLevelMockBeanCase {

    String describe() {
        return getClass().getSimpleName();
    }
}
