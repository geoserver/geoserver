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
public class Polygon implements Geometry {

    protected GeometryTypeEnum geometryType;

    public GeometryTypeEnum getGeometryType() {
        return geometryType;
    }

    public void setGeometryType(GeometryTypeEnum geometryType) {
        this.geometryType = geometryType;
    }
    
    private double[][][] rings;

    private SpatialReference spatialReference;

    public double[][][] getRings() {
        return rings;
    }

    public void setRings(double[][][] ring) {
        this.rings = ring;
    }

    public SpatialReference getSpatialReference() {
        return spatialReference;
    }

    public void setSpatialReference(SpatialReference spatialReference) {
        this.spatialReference = spatialReference;
    }

    public Polygon(double[][][] rings, SpatialReference spatialRef) {
        this.rings = rings;
        this.spatialReference = spatialRef;
        this.geometryType = GeometryTypeEnum.POLYGON;
    }
}
