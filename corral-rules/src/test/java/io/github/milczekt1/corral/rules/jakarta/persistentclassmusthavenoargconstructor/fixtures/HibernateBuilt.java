package io.github.milczekt1.corral.rules.jakarta.persistentclassmusthavenoargconstructor.fixtures;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.hibernate.annotations.EmbeddableInstantiator;

/** MUST IGNORE: an annotation type, not a mapped class; {@link DiscountRate} carries it. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@EmbeddableInstantiator(RateReader.class)
public @interface HibernateBuilt {
}
