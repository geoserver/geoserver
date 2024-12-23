/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import java.util.List;
import javax.annotation.Nullable;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.util.CloseableIterator;
import org.geotools.api.filter.Filter;
import org.springframework.security.core.Authentication;

/**
 * Provides the {@link SecureCatalogImpl} with directives on what the specified user can access.
 *
 * <p>Implementations should extend from {@link AbstractResourceAccessManager}.
 *
 * @author Andrea Aime - GeoSolutions
 */
public interface ResourceAccessManager {

    /**
     * Returns the access limits for the workspace and stores included in it. For specific resource access and published
     * resource access see the other two methods
     *
     * @return The access limits for this workspace, or null if there are no limits
     */
    public WorkspaceAccessLimits getAccessLimits(Authentication user, WorkspaceInfo workspace);

    /** Returns the access limits for the specified layer, or null if there are no limits. */
    public DataAccessLimits getAccessLimits(Authentication user, LayerInfo layer);

    /**
     * Returns the access limits for the specified layer accessed via the groups listed as containers (will be an empty
     * list for direct access), or null if there are no limits.
     */
    public default DataAccessLimits getAccessLimits(
            Authentication user, LayerInfo layer, List<LayerGroupInfo> containers) {
        return getAccessLimits(user, layer);
    }

    /** Returns the access limits for the specified resource, or null if there are no limits. */
    public DataAccessLimits getAccessLimits(Authentication user, ResourceInfo resource);

    /** Returns the access limits for the specified style, or null if there are no limits. */
    public StyleAccessLimits getAccessLimits(Authentication user, StyleInfo style);

    /** Returns the access limits for the specified layer group, or null if there are no limits. */
    public LayerGroupAccessLimits getAccessLimits(Authentication user, LayerGroupInfo layerGroup);

    /**
     * Returns the access limits for the specified layer group accessed via the groups listed as containers (will be an
     * empty list for direct access), or null if there are no limits, or null if there are no limits.
     */
    public default LayerGroupAccessLimits getAccessLimits(
            Authentication user, LayerGroupInfo layerGroup, List<LayerGroupInfo> containers) {
        return getAccessLimits(user, layerGroup);
    }

    /**
     * Returns a filter selecting only the objects authorized by the manager. May return {@code null} in which case the
     * caller is responsible for building a filter based on calls to the manager's other methods.
     */
    public @Nullable Filter getSecurityFilter(Authentication user, final Class<? extends CatalogInfo> clazz);

    /**
     * Checks if {@code user} has admin privileges on at least one workspace of {@code catalog}.
     *
     * <p>This default implementation will potentially traverse all workspaces on the {@code Catalog} until
     * {@link #getAccessLimits(Authentication, WorkspaceInfo)} returns {@link WorkspaceAccessLimits#isAdminable() ==
     * true}.
     *
     * <p>{@code ResourceAccessManager} implementations are encouraged to override this method with a more efficient
     * implementation whenever possible.
     */
    default boolean isWorkspaceAdmin(Authentication user, Catalog catalog) {
        try (CloseableIterator<WorkspaceInfo> workspaces = catalog.list(WorkspaceInfo.class, Filter.INCLUDE)) {
            while (workspaces.hasNext()) {
                WorkspaceInfo ws = workspaces.next();
                WorkspaceAccessLimits accessLimits = getAccessLimits(user, ws);
                if (accessLimits != null && accessLimits.isAdminable()) {
                    return true;
                }
            }
        }

        return false;
    }
}
