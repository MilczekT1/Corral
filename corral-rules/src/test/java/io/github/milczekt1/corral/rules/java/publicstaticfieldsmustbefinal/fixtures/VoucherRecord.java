package io.github.milczekt1.corral.rules.java.publicstaticfieldsmustbefinal.fixtures;

import javax.persistence.Entity;
import javax.persistence.Id;

/** MUST IGNORE: the javax entity {@link Voucher_} describes, with no public static field. */
@Entity
public class VoucherRecord {

    @Id
    private String code;

    public String getCode() {
        return code;
    }
}
