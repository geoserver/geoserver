/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0;

import java.util.List;
import java.util.Map;

import javax.media.jai.Interpolation;

import org.geotools.geometry.GeneralEnvelope;
import org.geotools.util.DateRange;
import org.geotools.util.NumberRange;
import org.opengis.filter.Filter;
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

    NumberRange<?> elevationSubset;

    Map<String, List<Object>> dimensionsSubset;

    CoordinateReferenceSystem outputCRS;

    Interpolation spatialInterpolation;

    Interpolation temporalInterpolation;

    Filter filter;

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

    public NumberRange<?> getElevationSubset() {
        return elevationSubset;
    }

    public void setElevationSubset(NumberRange<?> elevationSubset) {
        this.elevationSubset = elevationSubset;
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

    public Filter getFilter() {
        return filter;
    }

    public void setFilter(Filter filter) {
        this.filter = filter;
    }

    public Map<String, List<Object>> getDimensionsSubset() {
        return dimensionsSubset;
    }

    public void setDimensionsSubset(Map<String, List<Object>> dimensionsSubset) {
        this.dimensionsSubset = dimensionsSubset;
    }
}
