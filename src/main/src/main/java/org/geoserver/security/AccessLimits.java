/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import java.io.Serial;
import java.io.Serializable;
import java.util.Set;

/**
 * Base class for all AccessLimits declared by a {@link ResourceAccessManager}.
 *
 * <p>AccessLimits are used to limit the access to a resource (workspace, layer, style, catalog * resource). While the
 * hierarchy of access limits has well known classes matching the associated resource, a ResourceAccessManager can also
 * create subclasses of them, thus, if any customization to access limits is needed, one can clone the
 * {@link AccessLimits} object and change its settings. For this purpose, AccessLimits implements {@link Cloneable}.
 *
 * @author Andrea Aime - GeoSolutions
 */
public class AccessLimits implements Serializable, Cloneable {
    @Serial
    private static final long serialVersionUID = 8521276966116962954L;

    /** Gets the catalog mode for this layer */
    CatalogMode mode;

    /**
     * Optional tags used for targeted cache invalidation (tile caches, CDNs, WMS caches, etc.).
     * {@link ResourceAccessManager} implementations supporting tag-based invalidation may populate this with opaque
     * tags, that can then be used to drop tile caches affected by security rule changes. Deliberately excluded from
     * {@link #equals} and {@link #hashCode}: tags are invalidation metadata, not part of the content fingerprint.
     *
     * <p><strong>A tag must not contain a comma</strong>: tags are stored as a single comma-separated value in the tile
     * cache, so a comma in a tag breaks targeted invalidation. Comma-bearing tags are rejected.
     */
    private Set<String> securityTags;

    /** Builds a generic AccessLimits */
    public AccessLimits(CatalogMode mode) {
        this.mode = mode;
    }

    /** The catalog mode for this layer */
    public CatalogMode getMode() {
        return mode;
    }

    /** Tags used for targeted cache invalidation, or null if none. See {@link #securityTags}. */
    public Set<String> getSecurityTags() {
        return securityTags;
    }

    /**
     * Sets tags used for targeted cache invalidation. See {@link #securityTags}.
     *
     * @throws IllegalArgumentException if any tag contains a comma
     */
    public void setSecurityTags(Set<String> securityTags) {
        if (securityTags == null) {
            this.securityTags = null;
            return;
        }
        for (String tag : securityTags) {
            if (tag.indexOf(',') >= 0) {
                throw new IllegalArgumentException("Security tag must not contain a comma: " + tag);
            }
        }
        this.securityTags = Set.copyOf(securityTags);
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
