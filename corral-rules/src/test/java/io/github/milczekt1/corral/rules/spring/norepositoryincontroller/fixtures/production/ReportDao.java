package io.github.milczekt1.corral.rules.spring.norepositoryincontroller.fixtures.production;

import java.util.List;

/** SUPPORT: a hand-written DAO with neither {@code @Repository} nor a Spring Data supertype. */
public class ReportDao {

    public List<String> monthly() {
        return List.of("2026-09");
    }
}
