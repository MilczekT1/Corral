package io.github.milczekt1.corral.rules.testing.noconstructionmocking.fixtures;

import org.mockito.Answers;
import org.mockito.Mockito;

/**
 * MUST FLAG: {@code mockConstructionWithAnswer} is a name of its own rather than an overload, and
 * the erased sink leaves only the call clause able to see it.
 */
public class WithAnswerHandoffCaller {

    private final StringBuilder installed = new StringBuilder();

    public void install() {
        keep(Mockito.mockConstructionWithAnswer(StringBuilder.class, Answers.RETURNS_DEFAULTS));
    }

    public void keep(Object handle) {
        installed.append(handle);
    }
}
