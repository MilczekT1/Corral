package io.github.milczekt1.corral.rules.spring.norepositoryincontroller.fixtures.production;

import org.springframework.data.repository.CrudRepository;

/** SUPPORT: a Spring Data repository, reached through {@code CrudRepository}. */
public interface OrderRepository extends CrudRepository<OrderSummary, Long> {
}
