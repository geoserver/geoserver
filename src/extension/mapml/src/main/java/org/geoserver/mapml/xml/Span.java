/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlValue;
import java.util.List;

@XmlType
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "map-span", namespace = "http://www.w3.org/1999/xhtml")
public class Span {

    @XmlAttribute(name = "class")
    String clazz;

    @XmlValue
    protected List<String> coordinates;

    public Span(String clazz, List<String> coordinates) {
        this.clazz = clazz;
        this.coordinates = coordinates;
    }

    public Span() {}

    public String getClazz() {
        return clazz;
    }

    public void setClazz(String clazz) {
        this.clazz = clazz;
    }

    public List<String> getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(List<String> coordinates) {
        this.coordinates = coordinates;
    }
}
