package io.github.milczekt1.corral.rules.spring.norepositoryincontroller.fixtures;

import java.util.List;

/** Not an example: a hand-written DAO with neither {@code @Repository} nor a Spring Data supertype. */
public class ReportDao {

    public List<String> monthly() {
        return List.of("2026-09");
    }
}
