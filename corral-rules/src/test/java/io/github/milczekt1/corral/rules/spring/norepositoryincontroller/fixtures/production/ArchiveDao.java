package io.github.milczekt1.corral.rules.spring.norepositoryincontroller.fixtures.production;

import java.util.List;

/** SUPPORT: a DAO reaching {@code @Repository} only through a composed annotation. */
@PersistenceAdapter
public class ArchiveDao {

    public List<String> yearsOnFile() {
        return List.of("2025", "2026");
    }
}
