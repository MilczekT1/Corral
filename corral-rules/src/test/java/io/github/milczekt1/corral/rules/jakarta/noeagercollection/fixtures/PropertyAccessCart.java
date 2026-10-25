package io.github.milczekt1.corral.rules.jakarta.noeagercollection.fixtures;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import java.util.Set;

/** Property access: the mapping is on the getters, so MUST IGNORE the unannotated fields. */
@Entity
@Access(AccessType.PROPERTY)
public class PropertyAccessCart {

    private Long id;

    private Set<Product> items;

    private Set<Product> tags;

    @Id
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    /** MUST FLAG: declared {@code EAGER} on a getter. */
    @OneToMany(fetch = FetchType.EAGER)
    public Set<Product> getItems() {
        return items;
    }

    public void setItems(Set<Product> items) {
        this.items = items;
    }

    /** MUST IGNORE: declared {@code LAZY} on a getter. */
    @ManyToMany(fetch = FetchType.LAZY)
    public Set<Product> getTags() {
        return tags;
    }

    public void setTags(Set<Product> tags) {
        this.tags = tags;
    }
}
