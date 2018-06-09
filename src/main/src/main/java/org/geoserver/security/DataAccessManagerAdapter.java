/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMTSLayerInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geotools.util.logging.Logging;
import org.opengis.filter.Filter;
import org.springframework.security.core.Authentication;

/**
 * Adapts a {@link DataAccessManager} to the {@link ResourceAccessManager} interface
 *
 * @author Andrea Aime - GeoSolutions
 */
public class DataAccessManagerAdapter extends AbstractResourceAccessManager {
    static final Logger LOGGER = Logging.getLogger(DataAccessManagerAdapter.class);

    DataAccessManager delegate;

    /**
     * Builds a new adapter
     *
     * @param delegate
     */
    public DataAccessManagerAdapter(DataAccessManager delegate) {
        this.delegate = delegate;
    }

    public DataAccessLimits getAccessLimits(Authentication user, LayerInfo layer) {
        boolean read = delegate.canAccess(user, layer, AccessMode.READ);
        boolean write = delegate.canAccess(user, layer, AccessMode.WRITE);
        Filter readFilter = read ? Filter.INCLUDE : Filter.EXCLUDE;
        Filter writeFilter = write ? Filter.INCLUDE : Filter.EXCLUDE;
        return buildLimits(layer.getResource().getClass(), readFilter, writeFilter);
    }

    public DataAccessLimits getAccessLimits(Authentication user, ResourceInfo resource) {
        boolean read = delegate.canAccess(user, resource, AccessMode.READ);
        boolean write = delegate.canAccess(user, resource, AccessMode.WRITE);
        Filter readFilter = read ? Filter.INCLUDE : Filter.EXCLUDE;
        Filter writeFilter = write ? Filter.INCLUDE : Filter.EXCLUDE;
        return buildLimits(resource.getClass(), readFilter, writeFilter);
    }

    DataAccessLimits buildLimits(
            Class<? extends ResourceInfo> resourceClass, Filter readFilter, Filter writeFilter) {
        CatalogMode mode = delegate.getMode();

        // allow the secure catalog to avoid any kind of wrapping if there are no limits
        if ((readFilter == null || readFilter == Filter.INCLUDE)
                && (writeFilter == null
                        || writeFilter == Filter.INCLUDE
                        || WMSLayerInfo.class.isAssignableFrom(resourceClass)
                        || WMTSLayerInfo.class.isAssignableFrom(resourceClass)
                        || CoverageInfo.class.isAssignableFrom(resourceClass))) {
            return null;
        }

        // build the appropriate limit class
        if (FeatureTypeInfo.class.isAssignableFrom(resourceClass)) {
            return new VectorAccessLimits(mode, null, readFilter, null, writeFilter);
        } else if (CoverageInfo.class.isAssignableFrom(resourceClass)) {
            return new CoverageAccessLimits(mode, readFilter, null, null);
        } else if (WMSLayerInfo.class.isAssignableFrom(resourceClass)) {
            return new WMSAccessLimits(mode, readFilter, null, true);
        } else if (WMTSLayerInfo.class.isAssignableFrom(resourceClass)) {
            return new WMTSAccessLimits(mode, readFilter, null);
        } else {
            LOGGER.log(
                    Level.INFO,
                    "Warning, adapting to generic access limits for unrecognized resource type "
                            + resourceClass);
            return new DataAccessLimits(mode, readFilter);
        }
    }

    public WorkspaceAccessLimits getAccessLimits(Authentication user, WorkspaceInfo workspace) {
        boolean readable = delegate.canAccess(user, workspace, AccessMode.READ);
        boolean writable = delegate.canAccess(user, workspace, AccessMode.WRITE);
        boolean adminable = delegate.canAccess(user, workspace, AccessMode.ADMIN);

        CatalogMode mode = delegate.getMode();

        if (readable && writable) {
            if (AdminRequest.get() == null) {
                // not admin request, read+write means full acesss
                return null;
            }
        }
        return new WorkspaceAccessLimits(mode, readable, writable, adminable);
    }

    @SuppressWarnings("deprecation")
    @Override
    public Filter getSecurityFilter(Authentication user, Class<? extends CatalogInfo> clazz) {

        if (delegate.getMode() == CatalogMode.CHALLENGE)
            // If we're in CHALLENGE mode, everything should be visible
            return Predicates.acceptAll();
        else return super.getSecurityFilter(user, clazz);
    }
}
