package io.github.milczekt1.corral.rules.spring.notransactionalincontroller.fixtures;

import org.springframework.transaction.annotation.Transactional;

/** MUST IGNORE: not a controller; an interface annotated {@code @Transactional} at type level. */
@Transactional
public interface ExportOperations {

    String export(String format);
}
