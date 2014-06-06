/* Copyright (c) 2013 - 2014 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.opengeo.gsr.core.geometry;

import java.util.Set;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.referencing.ReferenceIdentifier;

/**
 * 
 * @author Juan Marin - OpenGeo
 * 
 */
public class Envelope implements Geometry {

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
    	
    	Set<ReferenceIdentifier> ids = envelope.getCoordinateReferenceSystem().getIdentifiers();
    	int EPSGid = -1;
    	for(ReferenceIdentifier id : ids) {
    		if (id.getCodeSpace().equalsIgnoreCase("EPSG")) {
    			EPSGid = Integer.parseInt(id.getCode());
    		}
    	}
    	
    	if(EPSGid < 0) {
    		this.spatialReference = new SpatialReferenceWKID(EPSGid);
    	}
    	else {
    		this.spatialReference = new SpatialReferenceWKT(envelope.getCoordinateReferenceSystem().toWKT());
    	}
    }

    public boolean isValid() {
        if (this.xmin <= this.xmax && this.ymin <= this.ymax) {
            return true;
        } else {
            return false;
        }
    }

}
