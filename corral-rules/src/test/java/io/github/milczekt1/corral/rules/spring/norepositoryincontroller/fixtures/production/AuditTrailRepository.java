package io.github.milczekt1.corral.rules.spring.norepositoryincontroller.fixtures.production;

import java.util.List;

/** SUPPORT: a project interface that only shares the {@code Repository} suffix. */
public interface AuditTrailRepository {

    List<String> recent();
}
