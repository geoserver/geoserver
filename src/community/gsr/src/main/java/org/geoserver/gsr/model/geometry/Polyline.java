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
public class Polyline extends Geometry {

    protected GeometryTypeEnum geometryType;

    @Override
    public GeometryTypeEnum getGeometryType() {
        return geometryType;
    }

    @Override
    public void setGeometryType(GeometryTypeEnum geometryType) {
        this.geometryType = geometryType;
    }

    private Number[][][] paths;

    private SpatialReference spatialReference;

    public Number[][][] getPaths() {
        return paths;
    }

    public void setPaths(Number[][][] paths) {
        this.paths = paths;
    }

    @Override
    public SpatialReference getSpatialReference() {
        return spatialReference;
    }

    @Override
    public void setSpatialReference(SpatialReference spatialReference) {
        this.spatialReference = spatialReference;
    }

    public Polyline(Number[][][] paths, SpatialReference spatialRef) {
        this.paths = paths;
        this.spatialReference = spatialRef;
        this.geometryType = GeometryTypeEnum.POLYLINE;
    }
}
