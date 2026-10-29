package io.github.milczekt1.corral.rules.java.noequalswithouthashcode.fixtures;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Set;

/** MUST IGNORE: {@code hashCode()} is inherited from {@code AbstractSet}, outside the imported classes. */
public final class TagSet extends AbstractSet<String> {

    private final Set<String> tags;

    public TagSet(Set<String> tags) {
        this.tags = Set.copyOf(tags);
    }

    @Override
    public Iterator<String> iterator() {
        return tags.iterator();
    }

    @Override
    public int size() {
        return tags.size();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Set<?> other && other.size() == size() && containsAll(other);
    }
}
