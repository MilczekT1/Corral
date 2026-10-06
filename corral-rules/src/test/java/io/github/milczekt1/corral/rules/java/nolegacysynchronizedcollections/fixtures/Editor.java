package io.github.milczekt1.corral.rules.java.nolegacysynchronizedcollections.fixtures;

/** MUST IGNORE: uses {@link UndoStack} without ever naming {@code Stack}. */
public class Editor {

    private final UndoStack undo = new UndoStack();

    public String type(String text) {
        undo.push(text);
        return undo.peek();
    }
}
