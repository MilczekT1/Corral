package io.github.milczekt1.corral.rules.spring.notransactionalincontroller.fixtures;

import org.springframework.web.bind.annotation.RestController;

/** MUST FLAG: implements an interface annotated {@code @Transactional} at type level. */
@RestController
public class ExportController implements ExportOperations {

    @Override
    public String export(String format) {
        return "report." + format;
    }
}
