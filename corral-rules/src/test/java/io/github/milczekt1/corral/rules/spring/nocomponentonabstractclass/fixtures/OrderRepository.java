package io.github.milczekt1.corral.rules.spring.nocomponentonabstractclass.fixtures;

import org.springframework.stereotype.Repository;

/** MUST IGNORE: an interface, abstract in bytecode, that another registrar turns into a bean. */
@Repository
public interface OrderRepository {

    long count();
}
