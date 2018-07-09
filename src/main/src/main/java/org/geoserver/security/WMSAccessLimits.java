/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import org.locationtech.jts.geom.MultiPolygon;
import org.opengis.filter.Filter;

/**
 * Describes access limits on a cascaded WMS layer
 *
 * @author Andrea Aime - GeoSolutions
 */
public class WMSAccessLimits extends DataAccessLimits {
    private static final long serialVersionUID = -6566842877723378894L;

    /** ROI on the returned images */
    MultiPolygon rasterFilter;

    /** Whether to allow feature info cascading or not */
    boolean allowFeatureInfo;

    /**
     * Builds a WMS limits
     *
     * @param filter Used as a CQL filter on servers supporting it and on cascaded feature info
     *     requests, and also to slice away feature info results
     * @param rasterFilter Used as a ROI on the returned data
     */
    public WMSAccessLimits(
            CatalogMode mode, Filter filter, MultiPolygon rasterFilter, boolean allowFeatureInfo) {
        super(mode, filter);
        this.rasterFilter = rasterFilter;
        this.allowFeatureInfo = allowFeatureInfo;
    }

    /** Acts as a ROI on the returned images */
    public MultiPolygon getRasterFilter() {
        return rasterFilter;
    }

    /** Wheter to allow GetFeatureInfo cascading or not */
    public boolean isAllowFeatureInfo() {
        return allowFeatureInfo;
    }

    @Override
    public String toString() {
        return "WMSAccessLimits [allowFeatureInfo="
                + allowFeatureInfo
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
        result = prime * result + (allowFeatureInfo ? 1231 : 1237);
        result = prime * result + ((rasterFilter == null) ? 0 : rasterFilter.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!super.equals(obj)) return false;
        if (getClass() != obj.getClass()) return false;
        WMSAccessLimits other = (WMSAccessLimits) obj;
        if (allowFeatureInfo != other.allowFeatureInfo) return false;
        if (rasterFilter == null) {
            if (other.rasterFilter != null) return false;
        } else if (!rasterFilter.equals(other.rasterFilter)) return false;
        return true;
    }
}
