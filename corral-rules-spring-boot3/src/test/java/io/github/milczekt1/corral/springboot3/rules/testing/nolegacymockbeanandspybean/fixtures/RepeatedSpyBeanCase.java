package io.github.milczekt1.corral.springboot3.rules.testing.nolegacymockbeanandspybean.fixtures;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.boot.test.mock.mockito.SpyBean;

/** MUST FLAG: a repeated class-level {@code @SpyBean} compiles to the {@code @SpyBeans} container. */
@SpyBean(Clock.class)
@SpyBean(ZoneId.class)
public class RepeatedSpyBeanCase {

    String describe() {
        return getClass().getSimpleName();
    }
}
