package io.github.milczekt1.corral.rules.spring.norepositoryincontroller.fixtures;

import java.util.List;
import org.springframework.web.bind.annotation.RestController;

/** MUST FLAG: a DAO meta-annotated, not annotated, {@code @Repository}. */
@RestController
public class ArchiveController {

    private final ArchiveDao archive;

    public ArchiveController(ArchiveDao archive) {
        this.archive = archive;
    }

    public List<String> years() {
        return archive.yearsOnFile();
    }
}
