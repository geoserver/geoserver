/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import org.locationtech.jts.geom.MultiPolygon;
import org.opengis.filter.Filter;

/**
 * Describes access limits on a cascaded WMTS layer
 *
 * @author Emanuele Tajariol (etj at geo-solutions dot it)
 */
public class WMTSAccessLimits extends DataAccessLimits {
    private static final long serialVersionUID = -6566842877723378894L;

    /** ROI on the returned images */
    MultiPolygon rasterFilter;

    /**
     * Builds a WMTS limits
     *
     * @param rasterFilter Used as a ROI on the returned data
     * @param readFilter generic filtering
     */
    public WMTSAccessLimits(CatalogMode mode, Filter readFilter, MultiPolygon rasterFilter) {
        super(mode, readFilter);
        this.rasterFilter = rasterFilter;
    }

    /** Acts as a ROI on the returned images */
    public MultiPolygon getRasterFilter() {
        return rasterFilter;
    }

    @Override
    public String toString() {
        return "WMTSAccessLimits [rasterFilter=" + rasterFilter + ", mode=" + mode + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((rasterFilter == null) ? 0 : rasterFilter.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!super.equals(obj)) return false;
        if (getClass() != obj.getClass()) return false;
        WMTSAccessLimits other = (WMTSAccessLimits) obj;
        if (rasterFilter == null) {
            if (other.rasterFilter != null) return false;
        } else if (!rasterFilter.equals(other.rasterFilter)) return false;
        return true;
    }
}
