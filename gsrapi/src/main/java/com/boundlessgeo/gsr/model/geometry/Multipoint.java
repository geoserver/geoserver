/* Copyright (c) 2013 - 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package com.boundlessgeo.gsr.model.geometry;

/**
 *
 * @author Juan Marin - OpenGeo
 *
 */
public class Multipoint extends Geometry {

    protected GeometryTypeEnum geometryType;

    public GeometryTypeEnum getGeometryType() {
        return geometryType;
    }

    public void setGeometryType(GeometryTypeEnum geometryType) {
        this.geometryType = geometryType;
    }

    private Number[][] points;

    public Number[][] getPoints() {
        return points;
    }

    public void setPoints(Number[][] points) {
        this.points = points;
    }

    public SpatialReference getSpatialReference() {
        return spatialReference;
    }

    public void setSpatialReference(SpatialReference spatialReference) {
        this.spatialReference = spatialReference;
    }

    public Multipoint(Number[][] coords, SpatialReference spatialReference) {
        this.points = coords;
        this.spatialReference = spatialReference;
        this.geometryType = GeometryTypeEnum.MULTIPOINT;
    }

}
