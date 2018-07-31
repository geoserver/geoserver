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
public class Polygon extends Geometry {

    protected GeometryTypeEnum geometryType;

    public GeometryTypeEnum getGeometryType() {
        return geometryType;
    }

    public void setGeometryType(GeometryTypeEnum geometryType) {
        this.geometryType = geometryType;
    }

    private Number[][][] rings;

    private SpatialReference spatialReference;

    public Number[][][] getRings() {
        return rings;
    }

    public void setRings(Number[][][] ring) {
        this.rings = ring;
    }

    public SpatialReference getSpatialReference() {
        return spatialReference;
    }

    public void setSpatialReference(SpatialReference spatialReference) {
        this.spatialReference = spatialReference;
    }

    public Polygon(Number[][][] rings, SpatialReference spatialRef) {
        this.rings = rings;
        this.spatialReference = spatialRef;
        this.geometryType = GeometryTypeEnum.POLYGON;
    }
}
