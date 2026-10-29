package io.github.milczekt1.corral.rules.java.noequalswithouthashcode.fixtures;

import java.util.List;

/** MUST FLAG: the abstract {@code hashCode()} that {@code List} re-declares implements nothing. */
public abstract class RouteList implements List<String> {

    @Override
    public boolean equals(Object o) {
        return o instanceof RouteList other && other.size() == size();
    }
}
