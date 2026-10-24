package io.github.milczekt1.corral.rules.jackson.staticobjectmappermustbefinal.fixtures;

/** MUST FLAG {@code auditMapper}: a project's own {@code ObjectMapper} subclass counts through its hierarchy. */
public class AuditLog {

    static AuditMapper auditMapper = new AuditMapper();

    static String entry(Object event) throws Exception {
        return auditMapper.writeValueAsString(event);
    }
}
