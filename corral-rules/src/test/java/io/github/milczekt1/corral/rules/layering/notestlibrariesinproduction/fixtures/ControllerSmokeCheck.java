package io.github.milczekt1.corral.rules.layering.notestlibrariesinproduction.fixtures;

import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;

/** MUST FLAG {@code org.springframework.boot..test..}: a Boot 4 test slice, two levels below Boot's root. */
@WebMvcTest
public class ControllerSmokeCheck {

    String describe() {
        return getClass().getSimpleName();
    }
}
