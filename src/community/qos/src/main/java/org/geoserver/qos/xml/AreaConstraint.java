/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.qos.xml;

import java.io.Serializable;

public class AreaConstraint implements Serializable {
    private static final long serialVersionUID = 1L;
    private Double minX, minY, MaxX, MaxY;

    public AreaConstraint() {}

    public AreaConstraint(Double minX, Double minY, Double maxX, Double maxY) {
        super();
        this.minX = minX;
        this.minY = minY;
        MaxX = maxX;
        MaxY = maxY;
    }

    public Double getMinX() {
        return minX;
    }

    public void setMinX(Double minX) {
        this.minX = minX;
    }

    public Double getMinY() {
        return minY;
    }

    public void setMinY(Double minY) {
        this.minY = minY;
    }

    public Double getMaxX() {
        return MaxX;
    }

    public void setMaxX(Double maxX) {
        MaxX = maxX;
    }

    public Double getMaxY() {
        return MaxY;
    }

    public void setMaxY(Double maxY) {
        MaxY = maxY;
    }
}
