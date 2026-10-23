package io.github.milczekt1.corral.rules.spring.notransactionalincontroller.fixtures;

/** MUST IGNORE: not a controller; an unannotated base class between a controller and an annotated one. */
public abstract class AbstractJournalEndpoint extends TransactionalAuditBase {
}
