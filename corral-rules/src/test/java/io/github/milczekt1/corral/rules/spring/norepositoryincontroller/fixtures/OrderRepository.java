package io.github.milczekt1.corral.rules.spring.norepositoryincontroller.fixtures;

import org.springframework.data.repository.CrudRepository;

/** Not an example: a Spring Data repository, reached through {@code CrudRepository}. */
public interface OrderRepository extends CrudRepository<OrderSummary, Long> {
}
