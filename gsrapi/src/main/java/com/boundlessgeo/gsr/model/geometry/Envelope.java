/* Copyright (c) 2013 - 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package com.boundlessgeo.gsr.model.geometry;

import com.boundlessgeo.gsr.translate.geometry.SpatialReferences;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.referencing.FactoryException;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 *
 * @author Juan Marin - OpenGeo
 *
 */
public class Envelope extends Geometry {

    protected GeometryTypeEnum geometryType;

    public GeometryTypeEnum getGeometryType() {
        return geometryType;
    }

    public void setGeometryType(GeometryTypeEnum geometryType) {
        this.geometryType = geometryType;
    }

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
        this.geometryType = GeometryTypeEnum.ENVELOPE;
    }

    public Envelope(ReferencedEnvelope envelope)
    {
        this.xmin = envelope.getMinX();
        this.xmax = envelope.getMaxX();
        this.ymin = envelope.getMinY();
        this.ymax = envelope.getMaxY();

        try {
            this.spatialReference = SpatialReferences.fromCRS(envelope.getCoordinateReferenceSystem());
        } catch (FactoryException e) {
            throw new RuntimeException(e);
        }
    }

    @JsonIgnore
    public boolean isValid() {
        return this.xmin <= this.xmax && this.ymin <= this.ymax;
    }

}
