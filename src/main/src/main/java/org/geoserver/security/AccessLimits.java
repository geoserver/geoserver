/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import java.io.Serializable;

/**
 * Base class for all AccessLimits declared by a {@link ResourceAccessManager}
 *
 * @author Andrea Aime - GeoSolutions
 */
public class AccessLimits implements Serializable {
    private static final long serialVersionUID = 8521276966116962954L;

    /** Gets the catalog mode for this layer */
    CatalogMode mode;

    /** Builds a generic AccessLimits */
    public AccessLimits(CatalogMode mode) {
        this.mode = mode;
    }

    /** The catalog mode for this layer */
    public CatalogMode getMode() {
        return mode;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((mode == null) ? 0 : mode.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        AccessLimits other = (AccessLimits) obj;
        if (mode == null) {
            if (other.mode != null) return false;
        } else if (!mode.equals(other.mode)) return false;
        return true;
    }
}
