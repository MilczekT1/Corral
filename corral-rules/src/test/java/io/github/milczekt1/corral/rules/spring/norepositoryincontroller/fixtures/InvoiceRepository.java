package io.github.milczekt1.corral.rules.spring.norepositoryincontroller.fixtures;

import java.util.Optional;
import org.springframework.data.repository.RepositoryDefinition;

/** Not an example: a Spring Data repository declared by annotation, extending nothing. */
@RepositoryDefinition(domainClass = OrderSummary.class, idClass = Long.class)
public interface InvoiceRepository {

    Optional<OrderSummary> findById(Long id);
}
