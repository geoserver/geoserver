/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.vfny.geoserver.wms.responses.map.htmlimagemap.holes;

import org.locationtech.jts.geom.Coordinate;

/**
 * Indexed coordinate. It allows to bind a coordinate to its position in a containing geometry
 * (through an index in the geometry points). It has two properties: - Position: coordinate - Index:
 * position in the geometry
 *
 * @author m.bartolomeoli
 */
public class Vertex {
    private Coordinate position;
    private int index;

    public Vertex(Coordinate position, int index) {
        this.position = position;
        this.index = index;
    }

    public Coordinate getPosition() {
        return position;
    }

    public void setPosition(Coordinate position) {
        this.position = position;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof Vertex)) return false;
        Vertex v = (Vertex) obj;
        return v.position.equals(position) && v.index == index;
    }

    public int hashCode() {
        return (position.hashCode() * 397) ^ index;
    }

    public String toString() {
        return ((position == null) ? "" : position.toString()) + " (" + index + ")";
    }
}
