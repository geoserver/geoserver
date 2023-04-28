/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import java.io.Serializable;

/**
 * Base class for all AccessLimits declared by a {@link ResourceAccessManager}.
 *
 * <p>AccessLimits are used to limit the access to a resource (workspace, layer, style, catalog *
 * resource). While the hierarchy of access limits has well known classes matching the associated
 * resource, a ResourceAccessManager can also create subclasses of them, thus, if any customization
 * to access limits is needed, one can clone the {@link AccessLimits} object and change its
 * settings. For this purpose, AccessLimits implements {@link Cloneable}.
 *
 * @author Andrea Aime - GeoSolutions
 */
public class AccessLimits implements Serializable, Cloneable {
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

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
