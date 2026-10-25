package io.github.milczekt1.corral.rules.jakarta.persistentclassmusthavenoargconstructor.fixtures;

import jakarta.persistence.Embeddable;
import org.hibernate.annotations.Instantiator;

/** MUST IGNORE: Hibernate builds it through the {@code @Instantiator} constructor. */
@Embeddable
public class Dimensions {

    private int width;
    private int height;

    @Instantiator({"width", "height"})
    public Dimensions(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public Dimensions(int side) {
        this(side, side);
    }

    public int area() {
        return width * height;
    }
}
