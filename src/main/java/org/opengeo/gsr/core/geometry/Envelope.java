/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.opengeo.gsr.core.geometry;

/**
 * 
 * @author Juan Marin - OpenGeo
 * 
 */
public class Envelope extends Geometry {

    private double xmin;

    private double ymin;

    private double xmax;

    private double ymax;

    private SpatialReference spatialReference;

    public double getXmin() {
        return xmin;
    }

    public void setXmin(double xmin) {
        this.xmin = xmin;
    }

    public double getYmin() {
        return ymin;
    }

    public void setYmin(double ymin) {
        this.ymin = ymin;
    }

    public double getXmax() {
        return xmax;
    }

    public void setXmax(double xmax) {
        this.xmax = xmax;
    }

    public double getYmax() {
        return ymax;
    }

    public void setYmax(double ymax) {
        this.ymax = ymax;
    }

    public SpatialReference getSpatialReference() {
        return spatialReference;
    }

    public void setSpatialReference(SpatialReference spatialReference) {
        this.spatialReference = spatialReference;
    }

    public Envelope(double xmin, double ymin, double xmax, double ymax,
            SpatialReference spatialReference) {
        this.xmin = xmin;
        this.xmax = xmax;
        this.ymin = ymin;
        this.ymax = ymax;
        this.spatialReference = spatialReference;
        this.geometryType = GeometryType.ENVELOPE;
    }

    public boolean isValid() {
        if (this.xmin <= this.xmax && this.ymin <= this.ymax) {
            return true;
        } else {
            return false;
        }
    }

}
