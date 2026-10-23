package io.github.milczekt1.corral.rules.spring.norepositoryincontroller.fixtures;

import java.util.List;

/** Not an example: a DAO reaching {@code @Repository} only through a composed annotation. */
@PersistenceAdapter
public class ArchiveDao {

    public List<String> yearsOnFile() {
        return List.of("2025", "2026");
    }
}
