package io.github.milczekt1.corral.rules.java.serializablemustdeclareserialversionuid.fixtures;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/** MUST FLAG: {@code Externalizable} streams carry a computed uid just the same. */
public class AuditEntry implements Externalizable {

    private String action;

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeUTF(action);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException {
        action = in.readUTF();
    }
}
