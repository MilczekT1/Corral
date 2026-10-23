package io.github.milczekt1.corral.rules.spring.notransactionalincontroller.fixtures;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** MUST IGNORE from test output, where it compiles; MUST FLAG once copied into production output. */
@RestController
public class TestOutputController {

    @Transactional
    @PostMapping("/probe")
    public long probe() {
        return System.nanoTime();
    }
}
