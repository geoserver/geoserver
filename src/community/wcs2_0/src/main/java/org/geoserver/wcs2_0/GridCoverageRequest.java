/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0;

import javax.media.jai.Interpolation;

import org.geotools.geometry.GeneralEnvelope;
import org.geotools.util.DateRange;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Class representing the coverage request in terms that can be directly applied to the internal
 * GridCoverage2D/GridReader model
 * 
 * @author Andrea Aime - GeoSolutions
 * 
 */
public class GridCoverageRequest {

    GeneralEnvelope spatialSubset;

    DateRange temporalSubset;

    CoordinateReferenceSystem outputCRS;

    Interpolation spatialInterpolation;
    
    Interpolation temporalInterpolation;

    public GeneralEnvelope getSpatialSubset() {
        return spatialSubset;
    }

    public void setSpatialSubset(GeneralEnvelope spatialSubset) {
        this.spatialSubset = spatialSubset;
    }

    public DateRange getTemporalSubset() {
        return temporalSubset;
    }

    public void setTemporalSubset(DateRange temporalSubset) {
        this.temporalSubset = temporalSubset;
    }

    public CoordinateReferenceSystem getOutputCRS() {
        return outputCRS;
    }

    public void setOutputCRS(CoordinateReferenceSystem outputCRS) {
        this.outputCRS = outputCRS;
    }

    public Interpolation getSpatialInterpolation() {
        return spatialInterpolation;
    }

    public void setSpatialInterpolation(Interpolation spatialInterpolation) {
        this.spatialInterpolation = spatialInterpolation;
    }

    public Interpolation getTemporalInterpolation() {
        return temporalInterpolation;
    }

    public void setTemporalInterpolation(Interpolation temporalInterpolation) {
        this.temporalInterpolation = temporalInterpolation;
    }
    
    
}
