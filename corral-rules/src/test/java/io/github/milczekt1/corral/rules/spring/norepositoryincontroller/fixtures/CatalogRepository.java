package io.github.milczekt1.corral.rules.spring.norepositoryincontroller.fixtures;

import java.util.List;
import org.springframework.data.repository.Repository;

/** Not an example: a Spring Data repository extending the {@code Repository} marker directly. */
public interface CatalogRepository extends Repository<OrderSummary, Long> {

    List<OrderSummary> findAll();
}
