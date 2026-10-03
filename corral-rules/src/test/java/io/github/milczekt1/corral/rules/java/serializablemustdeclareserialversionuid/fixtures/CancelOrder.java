package io.github.milczekt1.corral.rules.java.serializablemustdeclareserialversionuid.fixtures;

/** MUST FLAG: serializable through a project interface it declares, with no uid of its own. */
public class CancelOrder implements Command {

    private String orderReference;

    @Override
    public void execute() {
        orderReference = null;
    }
}
