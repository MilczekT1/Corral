package io.github.milczekt1.corral.rules.layering.notestlibrariesinproduction.fixtures;

import java.util.function.Supplier;
import org.easymock.EasyMock;

/** MUST FLAG {@code org.easymock..}: an EasyMock nice mock used as a no-op collaborator. */
public class AuditTrailStub {

    @SuppressWarnings("unchecked")
    Supplier<String> noOpAuditor() {
        return EasyMock.createNiceMock(Supplier.class);
    }
}
