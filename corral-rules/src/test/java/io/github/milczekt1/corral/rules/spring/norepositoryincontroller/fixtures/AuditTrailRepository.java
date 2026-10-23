package io.github.milczekt1.corral.rules.spring.norepositoryincontroller.fixtures;

import java.util.List;

/** Not an example: a project interface that only shares the {@code Repository} suffix. */
public interface AuditTrailRepository {

    List<String> recent();
}
