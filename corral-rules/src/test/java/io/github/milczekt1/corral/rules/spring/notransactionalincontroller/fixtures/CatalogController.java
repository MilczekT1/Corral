package io.github.milczekt1.corral.rules.spring.notransactionalincontroller.fixtures;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** MUST FLAG: a handler carrying a composed annotation meta-annotated {@code @Transactional}. */
@RestController
public class CatalogController {

    @ReadOnlyTransaction
    @GetMapping("/catalog")
    public List<String> browse() {
        return List.of("item");
    }
}
