package io.github.milczekt1.corral.rules.java.publicstaticfieldsmustbefinal.fixtures;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

/** MUST IGNORE: a javax static metamodel, whose attributes the specification requires to be {@code public static volatile}. */
@StaticMetamodel(VoucherRecord.class)
public abstract class Voucher_ {

    public static volatile SingularAttribute<VoucherRecord, String> code;
}
