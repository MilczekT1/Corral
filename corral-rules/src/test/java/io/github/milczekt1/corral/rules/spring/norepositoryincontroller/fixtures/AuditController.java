package io.github.milczekt1.corral.rules.spring.norepositoryincontroller.fixtures;

import java.util.List;
import org.springframework.web.bind.annotation.RestController;

/** MUST IGNORE: {@code AuditTrailRepository} carries neither marker, only the name. */
@RestController
public class AuditController {

    private final AuditTrailRepository auditTrail;

    public AuditController(AuditTrailRepository auditTrail) {
        this.auditTrail = auditTrail;
    }

    public List<String> recent() {
        return auditTrail.recent();
    }
}
