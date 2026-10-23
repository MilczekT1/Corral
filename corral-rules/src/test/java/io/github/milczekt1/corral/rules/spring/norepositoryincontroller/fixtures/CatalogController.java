package io.github.milczekt1.corral.rules.spring.norepositoryincontroller.fixtures;

import java.util.List;
import org.springframework.stereotype.Controller;

/** MUST FLAG: a plain {@code @Controller} on a repository extending Spring Data's {@code Repository} directly. */
@Controller
public class CatalogController {

    private final CatalogRepository catalog;

    public CatalogController(CatalogRepository catalog) {
        this.catalog = catalog;
    }

    public List<OrderSummary> list() {
        return catalog.findAll();
    }
}
