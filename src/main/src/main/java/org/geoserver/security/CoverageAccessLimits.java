/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import java.util.Arrays;
import org.locationtech.jts.geom.MultiPolygon;
import org.opengis.filter.Filter;
import org.opengis.parameter.GeneralParameterValue;

/**
 * Describes security limits on a raster layers
 *
 * @author Andrea Aime - GeoSolutions
 */
public class CoverageAccessLimits extends DataAccessLimits {

    private static final long serialVersionUID = -4269595923034528171L;

    /** Used as a ROI filter on raster data */
    MultiPolygon rasterFilter;

    /** Param overrides when grabbing a reader */
    transient GeneralParameterValue[] params;

    /**
     * Builds a raster limit
     *
     * @param readFilter The read filter, this has two purposes: if set to Filter.EXCLUDE it makes
     *     the entire layer non readable (hides, challenges), otherwise it's added to the reader
     *     parameter should the reader have a filter among its params (mosaic does for example)
     * @param rasterFilter Used as a ROI on the returned coverage
     * @param params Read parameters overrides
     */
    public CoverageAccessLimits(
            CatalogMode mode,
            Filter readFilter,
            MultiPolygon rasterFilter,
            GeneralParameterValue[] params) {
        super(mode, readFilter);
        this.rasterFilter = rasterFilter;
        this.params = params;
    }

    public MultiPolygon getRasterFilter() {
        return rasterFilter;
    }

    public GeneralParameterValue[] getParams() {
        return params;
    }

    @Override
    public String toString() {
        return "CoverageAccessLimits [params="
                + Arrays.toString(params)
                + ", rasterFilter="
                + rasterFilter
                + ", readFilter="
                + readFilter
                + ", mode="
                + mode
                + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Arrays.hashCode(params);
        result = prime * result + ((rasterFilter == null) ? 0 : rasterFilter.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!super.equals(obj)) return false;
        if (getClass() != obj.getClass()) return false;
        CoverageAccessLimits other = (CoverageAccessLimits) obj;
        if (!Arrays.equals(params, other.params)) return false;
        if (rasterFilter == null) {
            if (other.rasterFilter != null) return false;
        } else if (!rasterFilter.equals(other.rasterFilter)) return false;
        return true;
    }
}
