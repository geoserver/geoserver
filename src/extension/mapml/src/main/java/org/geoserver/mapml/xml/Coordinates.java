/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElementRef;
import jakarta.xml.bind.annotation.XmlMixed;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;

/**
 * Coordinates for a geometry. Can contain a mix of strings (the space separate coordinates bits) and {@link Span}
 * elements for bits that need to bit styled differently.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
        name = "",
        propOrder = {"coordinates"})
@XmlRootElement(name = "map-coordinates", namespace = "http://www.w3.org/1999/xhtml")
public class Coordinates {

    @XmlMixed
    @XmlElementRef(name = "map-span", type = Span.class, required = false)
    protected List<Object> coordinates;

    public Coordinates() {}

    public Coordinates(String coordinates) {
        this.coordinates = new ArrayList<>();
        this.coordinates.add(coordinates);
    }

    public Coordinates(List<Object> coordinates) {
        this.coordinates = new ArrayList<>();
        this.coordinates.addAll(coordinates);
    }

    public List<Object> getCoordinates() {
        if (coordinates == null) {
            coordinates = new ArrayList<>();
        }
        return this.coordinates;
    }
}
