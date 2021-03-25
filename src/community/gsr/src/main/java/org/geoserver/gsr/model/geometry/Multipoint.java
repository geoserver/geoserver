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
public class Multipoint extends Geometry {

    protected GeometryTypeEnum geometryType;

    @Override
    public GeometryTypeEnum getGeometryType() {
        return geometryType;
    }

    @Override
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

    @Override
    public SpatialReference getSpatialReference() {
        return spatialReference;
    }

    @Override
    public void setSpatialReference(SpatialReference spatialReference) {
        this.spatialReference = spatialReference;
    }

    public Multipoint(Number[][] coords, SpatialReference spatialReference) {
        this.points = coords;
        this.spatialReference = spatialReference;
        this.geometryType = GeometryTypeEnum.MULTIPOINT;
    }
}
