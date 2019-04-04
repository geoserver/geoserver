/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0;

import java.util.List;
import java.util.Map;
import javax.media.jai.Interpolation;
import org.geotools.coverage.grid.io.OverviewPolicy;
import org.geotools.util.DateRange;
import org.geotools.util.NumberRange;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Class representing the coverage request in terms that can be directly applied to the internal
 * GridCoverage2D/GridReader model
 *
 * @author Andrea Aime - GeoSolutions
 */
public class GridCoverageRequest {

    WCSEnvelope spatialSubset;

    DateRange temporalSubset;

    NumberRange<?> elevationSubset;

    Map<String, List<Object>> dimensionsSubset;

    CoordinateReferenceSystem outputCRS;

    Interpolation spatialInterpolation;

    Interpolation temporalInterpolation;

    OverviewPolicy overviewPolicy;

    Filter filter;

    List<SortBy> sortBy;

    public WCSEnvelope getSpatialSubset() {
        return spatialSubset;
    }

    public void setSpatialSubset(WCSEnvelope spatialSubset) {
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

    public List<SortBy> getSortBy() {
        return sortBy;
    }

    public void setSortBy(List<SortBy> sortBy) {
        this.sortBy = sortBy;
    }

    public Map<String, List<Object>> getDimensionsSubset() {
        return dimensionsSubset;
    }

    public void setDimensionsSubset(Map<String, List<Object>> dimensionsSubset) {
        this.dimensionsSubset = dimensionsSubset;
    }

    public OverviewPolicy getOverviewPolicy() {
        return overviewPolicy;
    }

    public void setOverviewPolicy(OverviewPolicy overviewPolicy) {
        this.overviewPolicy = overviewPolicy;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((dimensionsSubset == null) ? 0 : dimensionsSubset.hashCode());
        result = prime * result + ((elevationSubset == null) ? 0 : elevationSubset.hashCode());
        result = prime * result + ((filter == null) ? 0 : filter.hashCode());
        result = prime * result + ((outputCRS == null) ? 0 : outputCRS.hashCode());
        result = prime * result + ((overviewPolicy == null) ? 0 : overviewPolicy.hashCode());
        result =
                prime * result
                        + ((spatialInterpolation == null) ? 0 : spatialInterpolation.hashCode());
        result = prime * result + ((spatialSubset == null) ? 0 : spatialSubset.hashCode());
        result =
                prime * result
                        + ((temporalInterpolation == null) ? 0 : temporalInterpolation.hashCode());
        result = prime * result + ((temporalSubset == null) ? 0 : temporalSubset.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        GridCoverageRequest other = (GridCoverageRequest) obj;
        if (dimensionsSubset == null) {
            if (other.dimensionsSubset != null) return false;
        } else if (!dimensionsSubset.equals(other.dimensionsSubset)) return false;
        if (elevationSubset == null) {
            if (other.elevationSubset != null) return false;
        } else if (!elevationSubset.equals(other.elevationSubset)) return false;
        if (filter == null) {
            if (other.filter != null) return false;
        } else if (!filter.equals(other.filter)) return false;
        if (outputCRS == null) {
            if (other.outputCRS != null) return false;
        } else if (!outputCRS.equals(other.outputCRS)) return false;
        if (overviewPolicy != other.overviewPolicy) return false;
        if (spatialInterpolation == null) {
            if (other.spatialInterpolation != null) return false;
        } else if (!spatialInterpolation.equals(other.spatialInterpolation)) return false;
        if (spatialSubset == null) {
            if (other.spatialSubset != null) return false;
        } else if (!spatialSubset.equals(other.spatialSubset)) return false;
        if (temporalInterpolation == null) {
            if (other.temporalInterpolation != null) return false;
        } else if (!temporalInterpolation.equals(other.temporalInterpolation)) return false;
        if (temporalSubset == null) {
            if (other.temporalSubset != null) return false;
        } else if (!temporalSubset.equals(other.temporalSubset)) return false;
        return true;
    }
}
