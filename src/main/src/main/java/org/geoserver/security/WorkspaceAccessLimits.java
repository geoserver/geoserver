/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import org.geoserver.platform.GeoServerExtensions;

/**
 * Access limits to a workspace (the write flag controls also direct access to data stores, though
 * normally only configuration code should be playing directy with stores)
 *
 * @author Andrea Aime - GeoSolutions
 */
public class WorkspaceAccessLimits extends AccessLimits {
    private static final long serialVersionUID = -1852838160677767466L;

    boolean readable;

    boolean writable;

    boolean adminable;

    public WorkspaceAccessLimits(CatalogMode mode, boolean readable, boolean writable) {
        this(mode, readable, writable, isAuthenticatedAsAdmin());
    }

    private static boolean isAuthenticatedAsAdmin() {

        return GeoServerExtensions.bean(GeoServerSecurityManager.class)
                .checkAuthenticationForAdminRole();
    }

    public WorkspaceAccessLimits(
            CatalogMode mode, boolean readable, boolean writable, boolean adminable) {
        super(mode);
        this.readable = readable;
        this.writable = writable;
        this.adminable = adminable;
    }

    public boolean isReadable() {
        return readable;
    }

    public boolean isWritable() {
        return writable;
    }

    public boolean isAdminable() {
        return adminable;
    }

    @Override
    public String toString() {
        return "WorkspaceAccessLimits [readable="
                + readable
                + ", writable="
                + writable
                + ", adminable = "
                + adminable
                + ", mode="
                + mode
                + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (readable ? 1231 : 1237);
        result = prime * result + (writable ? 1231 : 1237);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        WorkspaceAccessLimits other = (WorkspaceAccessLimits) obj;
        if (readable != other.readable) return false;
        if (writable != other.writable) return false;
        return true;
    }
}
