package io.github.milczekt1.corral.rules.spring.norepositoryincontroller.fixtures;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Repository;

/** Not an example: a hand-written DAO carrying the {@code @Repository} stereotype and no Spring Data supertype. */
@Repository
public class LedgerDao {

    private final List<String> entries = new ArrayList<>();

    public void append(String entry) {
        entries.add(entry);
    }
}
