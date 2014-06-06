/* Copyright (c) 2013 - 2014 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.opengeo.gsr.core.geometry;

/**
 * 
 * @author Juan Marin - OpenGeo
 * 
 */
public class Polyline implements Geometry {

    protected GeometryTypeEnum geometryType;

    public GeometryTypeEnum getGeometryType() {
        return geometryType;
    }

    public void setGeometryType(GeometryTypeEnum geometryType) {
        this.geometryType = geometryType;
    }
    
    private double[][][] paths;

    private SpatialReference spatialReference;

    public double[][][] getPaths() {
        return paths;
    }

    public void setPaths(double[][][] paths) {
        this.paths = paths;
    }

    public SpatialReference getSpatialReference() {
        return spatialReference;
    }

    public void setSpatialReference(SpatialReference spatialReference) {
        this.spatialReference = spatialReference;
    }

    public Polyline(double[][][] paths, SpatialReference spatialRef) {
        this.paths = paths;
        this.spatialReference = spatialRef;
        this.geometryType = GeometryTypeEnum.POLYLINE;
    }
}
