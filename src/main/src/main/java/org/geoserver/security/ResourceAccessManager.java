/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.opengis.filter.Filter;
import org.springframework.security.core.Authentication;

/**
 * Provides the {@link SecureCatalogImpl} with directives on what the specified user can access.
 * <p>
 * Implementations should extend from {@link AbstractResourceAccessManager}.
 * </p>
 * @author Andrea Aime - GeoSolutions
 */
public interface ResourceAccessManager {

    /**
     * Returns the access limits for the workspace and stores included in it. For specific resource
     * access and published resource access see the other two methods
     * 
     * @param user
     * @param workspace
     * @return The access limits for this workspace, or null if there are no limits
     */
    public WorkspaceAccessLimits getAccessLimits(Authentication user, WorkspaceInfo workspace);

    /**
     * Returns the access limits for the specified layer, or null if there are no limits.
     */
    public DataAccessLimits getAccessLimits(Authentication user, LayerInfo layer);

    /**
     * Returns the access limits for the specified resource, or null if there are no limits.
     */
    public DataAccessLimits getAccessLimits(Authentication user, ResourceInfo resource);

    /**
     * Returns the access limits for the specified style, or null if there are no limits.
     */
    public StyleAccessLimits getAccessLimits(Authentication user, StyleInfo style);

    /**
     * Returns the access limits for the specified layer group, or null if there are no limits.
     */
    public LayerGroupAccessLimits getAccessLimits(Authentication user, LayerGroupInfo layerGroup);
    
    /**
     * Returns a Filter which removes catalog objects which this manager would hide.
     * @param user
     * @param clazz
     * @return
     */
    public Filter getSecurityFilter(Authentication user, final Class<? extends CatalogInfo> clazz);

}
