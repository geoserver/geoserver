/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAnyElement;
import jakarta.xml.bind.annotation.XmlMixed;
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
public class Coordinates {

    @XmlMixed
    @XmlAnyElement(lax = true)
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
