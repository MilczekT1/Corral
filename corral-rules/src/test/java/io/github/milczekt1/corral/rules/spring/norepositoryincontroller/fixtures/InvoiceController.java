package io.github.milczekt1.corral.rules.spring.norepositoryincontroller.fixtures;

import java.util.Optional;
import org.springframework.web.bind.annotation.RestController;

/** MUST FLAG: a {@code @RepositoryDefinition} repository, which has neither {@code @Repository} nor a Spring Data supertype. */
@RestController
public class InvoiceController {

    private final InvoiceRepository invoices;

    public InvoiceController(InvoiceRepository invoices) {
        this.invoices = invoices;
    }

    public Optional<OrderSummary> show(long id) {
        return invoices.findById(id);
    }
}
