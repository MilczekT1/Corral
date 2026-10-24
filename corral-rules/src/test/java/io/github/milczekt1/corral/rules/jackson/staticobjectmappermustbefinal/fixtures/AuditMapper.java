package io.github.milczekt1.corral.rules.jackson.staticobjectmappermustbefinal.fixtures;

import com.fasterxml.jackson.databind.ObjectMapper;

/** MUST IGNORE: a project's own mapper subclass, whose own static fields are not mappers. */
public class AuditMapper extends ObjectMapper {

    private static final long serialVersionUID = 1L;

    private static int instances;

    public AuditMapper() {
        instances++;
        findAndRegisterModules();
    }
}
