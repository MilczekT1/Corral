package io.github.milczekt1.corral.rules.jackson.noannotationsonentity.fixtures;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/** MUST FLAG {@code weight}: a Jackson module annotation outside the core annotation packages. */
@Entity
public class Parcel {

    @Id
    private Long id;

    @JacksonXmlProperty(localName = "weight_kg")
    private double weight;
}
