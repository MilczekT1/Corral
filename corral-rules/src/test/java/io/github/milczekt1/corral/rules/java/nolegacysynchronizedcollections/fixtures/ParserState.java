package io.github.milczekt1.corral.rules.java.nolegacysynchronizedcollections.fixtures;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Stack;
import java.util.Vector;

/** MUST FLAG the {@code new Vector<>()} behind a {@code List} field and the {@code new Stack<>()}; the {@code ArrayDeque} is fine. */
public class ParserState {

    private final List<String> tokens = new Vector<>();

    public int depth(String expression) {
        Stack<Character> open = new Stack<>();
        Deque<Character> closed = new ArrayDeque<>();
        for (char c : expression.toCharArray()) {
            if (c == '(') {
                open.push(c);
            } else if (c == ')') {
                closed.push(c);
            }
        }
        tokens.add(expression);
        return open.size() - closed.size();
    }
}
