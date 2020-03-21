/* Copyright (c) 2013 - 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package com.boundlessgeo.gsr.model.geometry;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 *
 * @author Juan Marin - OpenGeo
 *
 */

public class Point extends Geometry {

    protected GeometryTypeEnum geometryType;

    @JsonIgnore
    public GeometryTypeEnum getGeometryType() {
        return geometryType;
    }

    public void setGeometryType(GeometryTypeEnum geometryType) {
        this.geometryType = geometryType;
    }

    private Number x;

    private Number y;

    private SpatialReference spatialReference;

    public Number getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public Number getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public SpatialReference getSpatialReference() {
        return spatialReference;
    }

    public void setSpatialReference(SpatialReference spatialReference) {
        this.spatialReference = spatialReference;
    }

    public Point(Number x, Number y, SpatialReference spatialReference) {
        this.x = x;
        this.y = y;
        this.spatialReference = spatialReference;
        this.geometryType = GeometryTypeEnum.POINT;
    }

}
