package io.github.milczekt1.corral.rules.jakarta.persistentclassmusthavenoargconstructor.fixtures;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/** MUST FLAG its inner {@link Bay}, the example; MUST IGNORE the outer class, which is not mapped. */
public class Warehouse {

    private final String name;

    public Warehouse(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    /** MUST FLAG: a non-static inner class's implicit constructor takes the outer instance. */
    @Entity
    public class Bay {

        @Id
        private Long id;

        public String warehouseName() {
            return name;
        }
    }
}
