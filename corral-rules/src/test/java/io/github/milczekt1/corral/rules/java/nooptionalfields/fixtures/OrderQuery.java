package io.github.milczekt1.corral.rules.java.nooptionalfields.fixtures;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

/** MUST FLAG {@code name}; MUST IGNORE the nullable {@code reference} and the {@code List<Optional<String>>}, whose raw type is {@code List}. */
public class OrderQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    private Optional<String> name = Optional.empty();

    private String reference;

    private List<Optional<String>> tags = List.of();

    public String describe() {
        return name.orElse("") + tags.size();
    }

    public Optional<String> reference() {
        return Optional.ofNullable(reference);
    }
}
