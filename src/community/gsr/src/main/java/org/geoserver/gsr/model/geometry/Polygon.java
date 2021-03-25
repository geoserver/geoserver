/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/* Copyright (c) 2013 - 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gsr.model.geometry;

/** @author Juan Marin - OpenGeo */
public class Polygon extends Geometry {

    protected GeometryTypeEnum geometryType;

    @Override
    public GeometryTypeEnum getGeometryType() {
        return geometryType;
    }

    @Override
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

    @Override
    public SpatialReference getSpatialReference() {
        return spatialReference;
    }

    @Override
    public void setSpatialReference(SpatialReference spatialReference) {
        this.spatialReference = spatialReference;
    }

    public Polygon(Number[][][] rings, SpatialReference spatialRef) {
        this.rings = rings;
        this.spatialReference = spatialRef;
        this.geometryType = GeometryTypeEnum.POLYGON;
    }
}
