/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.springframework.security.core.Authentication;

/**
 * Data access manager provides the {@link SecureCatalogImpl} with directives on what the specified
 * user can access.
 *
 * @author Andrea Aime - TOPP
 * @deprecated Use {@link ResourceAccessManager} instead
 */
public interface DataAccessManager {

    /** Returns the security mode in which the secure catalog must operate */
    public CatalogMode getMode();

    /** Returns true if user can access the workspace in the specified mode */
    public boolean canAccess(Authentication user, WorkspaceInfo workspace, AccessMode mode);

    /** Returns true if user can access the layer in the specified mode */
    public boolean canAccess(Authentication user, LayerInfo layer, AccessMode mode);

    /** Returns true if user can access the resource in the specified mode */
    public boolean canAccess(Authentication user, ResourceInfo resource, AccessMode mode);
}
