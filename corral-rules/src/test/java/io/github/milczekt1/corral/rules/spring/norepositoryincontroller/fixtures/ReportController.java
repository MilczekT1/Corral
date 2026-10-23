package io.github.milczekt1.corral.rules.spring.norepositoryincontroller.fixtures;

import java.util.List;
import org.springframework.stereotype.Controller;

/** MUST IGNORE: a DAO with neither repository marker. */
@Controller
public class ReportController {

    private final ReportDao reports;

    public ReportController(ReportDao reports) {
        this.reports = reports;
    }

    public List<String> monthly() {
        return reports.monthly();
    }
}
